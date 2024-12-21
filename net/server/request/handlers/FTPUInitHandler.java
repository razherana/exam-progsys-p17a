package net.server.request.handlers;

import java.io.*;
import java.net.Socket;
import java.util.*;

import net.base.Body;
import net.base.Handler;
import net.base.Header;
import net.client.request.headers.FTPUChunkHeader;
import net.client.request.headers.FTPUInitHeader;
import net.server.Server;
import net.server.ftp.FTPServerUpload;
import net.server.ftp.UploadDetails;
import net.server.request.headers.Status;
import net.server.response.headers.ResponseFTPUInitHeader;

public class FTPUInitHandler extends Handler {
    private static final int MAX_TRANSFERS = 50;
    private static final int EVICTION_COUNT = 20;

    final private static PriorityQueue<Map.Entry<String, UploadDetails>> evictionQueue = new PriorityQueue<>(
            Comparator.comparingDouble((Map.Entry<String, UploadDetails> entry) -> entry.getValue().getProgress())
                    .thenComparingLong(entry -> entry.getValue().getTimestamp()));

    final private FTPServerUpload ftpServer;

    private UploadDetails transferDetail;

    public FTPUInitHandler(Socket socket, FTPServerUpload ftpServer) {
        super(socket);
        this.ftpServer = ftpServer;
    }

    public FTPServerUpload getFtpServer() { return ftpServer; }

    public void setTransferDetail(UploadDetails transferDetail) { this.transferDetail = transferDetail; }

    public UploadDetails getTransferDetail() { return transferDetail; }

    @Override
    public boolean isHandle(Header header, Body body) {
        return header.getMethodUniqId().equals(FTPUInitHeader.class.getName());
    }

    @Override
    public void handle(Header header, Body body) {
        if (header.getMethodUniqId().equals(FTPUInitHeader.class.getName())) {
            handleInit(header, body);
        } else if (header.getMethodUniqId().equals(FTPUChunkHeader.class.getName())) {
            handleChunk(header, body);
        }
    }

    public void cleanupTransfer() {
        ftpServer.getFtpChunkHandler().flushFlux();
        ftpServer.getFtpChunkHandler().flushMapFlux();
        String transferId = getTransferDetail().getTransferId();
        File cacheFile = new File(Server.CACHE_DIRECTORY + "/" + transferId + ".cache");
        if (cacheFile.exists() && cacheFile.delete()) {
            System.out.println("Deleted cache file for transfer " + transferId);
        }
    }

    private void handleInit(Header header, Body body) {
        FTPUInitHeader ftpHeader = (FTPUInitHeader) header;
        String transferId = UUID.randomUUID().toString();
        UploadDetails details = new UploadDetails(ftpHeader.getFilePath(), ftpHeader.getSize(),
                ftpHeader.getCountChunk(), ftpHeader.getParts(), transferId, ftpHeader.getPacketSize());

        setSocketInstance(ftpServer.getSocket());

        details.initMapFile();

        setTransferDetail(details);
        evictionQueue.add(Map.entry(transferId, details));

        if (evictionQueue.size() > MAX_TRANSFERS)
            evictTransfers();

        ResponseFTPUInitHeader responseFTPInitHeader = new ResponseFTPUInitHeader(transferId, ftpHeader.getUuid());
        System.out.println("FTP Init successful. Transfer ID: " + transferId);

        getTransferDetail().writeFileInfo();

        try {
            sendPacket(responseFTPInitHeader, Body.EMPTY_BODY, getSocketInstance());
        } catch (IOException e) {
            cli.ServerCli.writeOutput(e.getMessage());
        }
    }

    private void handleChunk(Header header, Body body) {
        if (!(header instanceof FTPUChunkHeader chunkHeader))
            throw new RuntimeException(
                    "The header of the chunk is not a FTPChunkHeader but a " + header.getClass().getName());
        String transferId = Objects.toString(Objects.requireNonNull(chunkHeader.getTransferId()));
        int chunkId = chunkHeader.getChunkId();

        UploadDetails details = getTransferDetail();

        if (details == null) {
            details = retrieveFromCache(transferId);

            if (details != null) {
                setTransferDetail(details);
                evictionQueue.add(Map.entry(transferId, details));
                System.out.println("Restored transfer " + transferId + " from cache.");
            } else {
                // Respond with an error if the transfer cannot be found
                try {
                    sendPacket(Status.ERR.getHeader("Transfer ID : " + transferId + " not found"), socketInstance);
                } catch (IOException e) {
                    cli.ServerCli.writeOutput(e.getMessage());
                }
                System.err.println("Error: Transfer ID not found: " + transferId);
                return;
            }
        }

        // Update the chunk progress
        details.addChunk(chunkId);
        details.incrementChunksSent();
        details.updateTimestamp();

        System.out.println("Received chunk " + chunkId + " for transfer " + transferId);
    }

    private UploadDetails retrieveFromCache(String transferId) {
        File cacheFile = new File(Server.CACHE_DIRECTORY + "/" + transferId + ".cache");
        if (!cacheFile.exists())
            return null;

        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(cacheFile))) {
            return (UploadDetails) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error reading cache file for transfer " + transferId + ": " + e.getMessage());
            return null;
        }
    }

    private void evictTransfers() {
        int count = 0;

        while (count < EVICTION_COUNT && !evictionQueue.isEmpty()) {
            Map.Entry<String, UploadDetails> entry = evictionQueue.poll();
            String transferId = entry.getKey();
            UploadDetails details = entry.getValue();

            File cacheFile = new File(Server.CACHE_DIRECTORY + "/" + transferId + ".cache");
            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(cacheFile))) {
                out.writeObject(details);
                setTransferDetail(null);
                count++;
                System.out.println("Evicted transfer " + transferId + " to file " + cacheFile.getPath());
            } catch (IOException e) {
                System.err.println("Error writing to cache file: " + e.getMessage());
            }
        }
    }
}

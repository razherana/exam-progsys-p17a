<?php

$file1 = file_get_contents($argv[1]);
$file2 = file_get_contents($argv[2]);
if(($s1 = strlen($file1)) != ($s2 = strlen($file2))) {
	echo "$s1 - $s2";
	return;
}
for($i = 1; $i < strlen($file1); $i++) {
	if($file1[$i] != $file2[$i]) {
		echo $i;
		break;
	}
}

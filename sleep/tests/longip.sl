$ip = join('.', unpack("B4", pack("I", '3232235777')));
$longip = unpack("I-", pack("B4", split('\.', "192.168.1.1")))[0];

println("$ip and $longip");

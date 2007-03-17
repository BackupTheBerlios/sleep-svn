sub check
{
   return;
}

debug(15);

fork({
   check("within fork");
});

sleep(1000);

check("outside of fork");

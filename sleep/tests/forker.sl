sub check
{
   return;
}

debug(15);

fork({
   check("within fork");
});

check("outside of fork");

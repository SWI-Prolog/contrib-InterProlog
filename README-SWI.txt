---+ About this version

This  version  is  InterProlog  is  slightly  adjusted  version  of  the
original. The original is available from this address:

  * http://www.declarativa.com/interprolog/

The changes can best be viewed in   the  git browser, which is available
at:

  * http://www.swi-prolog.org/git/contrib/InterProlog.git

---+ SWI-Prolog version issues

The InterProlog website indicates  the  InterProlog   does  not  work on
SWI-Prolog 5.10.5.  There are two problems:

  1. The file com/declarativa/interprolog/swi/interprolog.pl defines
  initialization/1.  This is since long a built-in ISO predicate.  The
  definition must be removed.

  2. The predicate fields//3 in com/declarativa/interprolog/swi/interprolog.pl
  contains a statement "Count=Count". This is a no-op, and is translated
  as 'true' since 5.9.x, but there is an error in the variable
  allocation for this particular construct.  There are two ways out

    * Replace the Count=Count by true and rebuild the jar (e.g. using
    unixScripts/build.sh).
    * Make sure to have this patch on SWI-Prolog:

	- http://www.swi-prolog.org/git/pl-devel.git/commit/5940793441b0d7073cf8b03db91383e4cf63c2cd

      This patch will be in 5.11.29 and 5.10.6


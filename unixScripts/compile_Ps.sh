cd ..

if [ -x "$1/xsb" ]; then
  $1/xsb -e "compile('com/declarativa/interprolog/xsb/interprolog.P'), compile('com/declarativa/interprolog/gui/visualization.P'), compile('com/declarativa/interprolog/tests.P'), halt."
else
  echo "WARNING: no XSB; skipped compilation of XSB Prolog files"
fi

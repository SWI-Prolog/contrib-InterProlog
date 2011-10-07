# Initialise directories for the various components. This script looks
# in $PATH.  If the executable is not in $PATH or you want to select
# a specific version respectless of $PATH, change the value of the
# corresponding variable to an absolute directory path.

dir_for_exe()
{ echo $(dirname $(which "$1"))
}

JAVA_BIN="$(dir_for_exe java)"
XSB_BIN_DIRECTORY="$(dir_for_exe xsb)"
SWI_BIN_DIRECTORY="$(dir_for_exe swipl)"
YAP_BIN_DIRECTORY="$(dir_for_exe yap)"

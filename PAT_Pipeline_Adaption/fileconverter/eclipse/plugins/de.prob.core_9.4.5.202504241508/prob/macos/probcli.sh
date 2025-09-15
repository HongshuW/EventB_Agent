#!/bin/bash
PROBCOMMAND=probcli
# use following line if you wish ProB2 to use probproxy and start probcli -s 8888 from source separately:
# PROBCOMMAND=probproxy
INTERRUPT_COMMAND=lib/send_user_interrupt

# Shell wrapper for PROBCOMMAND
# dirname
dirname=`dirname "$0"`

ulimit -d unlimited

chmod a+x "$dirname/$PROBCOMMAND"
# send_user_interrupt isn't present in ProB Tcl/Tk binary distributions,
# only in source builds and the probcli distributions for ProB 2.
if [ -f "$dirname/$INTERRUPT_COMMAND" ]
then
	chmod a+x "$dirname/$INTERRUPT_COMMAND"
fi
# When running with SICStus 4.4, uncomment this to fix problems with timeouts:
##export SP_TIMEOUT_IMPLEMENTATON="legacy"
exec "$dirname/$PROBCOMMAND" "$@"

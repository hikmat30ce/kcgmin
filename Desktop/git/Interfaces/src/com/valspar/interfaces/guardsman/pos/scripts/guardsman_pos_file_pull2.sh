#/usr/bin/sh
#
# Downloads file from ftp.valspar.com and unencrypts it
#
# MM
# 11/02/04
#
# RL - 12/19/05 1)Add ftp option. 2)delete processed files
# RL - 03/06/06 1)Renamed scripts 2)Not processing right away, just moving and decrypt
#     3) Corrected path names in the guardsman_pos.list
# RL - 09/13/10 Added count so that there can be multiple files for
#     one retailer without rewriting over itself.
#
# Create environment variables
#
export PATH=/opt/iexpress/gnupg/bin:/com/valspar/guardsman/scripts:$PATH
export SCRIPT_HOME=/com/valspar/guardsman/scripts
export GUARDSMAN_HOME=/data/guardsman_pos/transfers
#export GUARDSMAN_FTP=ftp.valspar.com
export GUARDSMAN_FTP=$1
export DIRDATETIME=`date "+%m%d20%y-%H%M"`

# Execute the SFTP commands
# 
sftp -b $SCRIPT_HOME/guardsman_sears.list king.valspar.com
sftp -b $SCRIPT_HOME/guardsman_pos.list $GUARDSMAN_FTP

#
# Process all files downloaded
#

COUNT=0

for i in `find $GUARDSMAN_HOME -type f -name 'valspar*.txt' | grep -v archive` 
	do
	COUNT=`expr $COUNT + 1`
	export BASENAME=`basename $i`
	export ENCRYPT_DIR=`dirname $i`
	cd $ENCRYPT_DIR
	. ./encrypt.type
#
# check to see if the file is supposed to be pgp encrypted
                if [ $ENCRYPT_TYPE = pgp ]; then
                cp $BASENAME valspar${DIRDATETIME}${COUNT}.pgp
                gpg -o valspar_use${DIRDATETIME}${COUNT}.tmp --decrypt valspar${DIRDATETIME}${COUNT}.pgp
                dos2ux valspar_use${DIRDATETIME}${COUNT}.tmp > valspar_use${DIRDATETIME}${COUNT}.txt
                rm valspar_use${DIRDATETIME}${COUNT}.tmp
                mv $BASENAME $ENCRYPT_DIR/archive
                rm -f valspar*.pgp
                fi
#
# check to see if the file is supposed to be sent via sftp
		if [ $ENCRYPT_TYPE = sftp ]; then
		export ENCRYPT_FILE=`dos2ux $BASENAME > valspar_use${DIRDATETIME}${COUNT}.txt`
		mv $BASENAME $ENCRYPT_DIR/archive
		fi
#
# check to see if the file is supposed to be sent via FTP
		if [ $ENCRYPT_TYPE = ftp ]; then
		export ENCRYPT_FILE=`dos2ux $BASENAME > valspar_use${DIRDATETIME}${COUNT}.txt`
		mv $BASENAME $ENCRYPT_DIR/archive
		fi
#
cd $GUARDSMAN_HOME
done		

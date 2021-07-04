MODULE=$1
RAW_DIR=${PWD}/../app/src/main/res/raw
ZIP_FILE=zip${MODULE}
CRC_FILE=crc${MODULE}

echo "Making: $ZIP_FILE"

if [ ! -d $MODULE ]
then
    echo "Module doesn't exists"
    exit
fi

cd $MODULE

zip -r ${ZIP_FILE}.zip *

mv ${ZIP_FILE}.zip $RAW_DIR

cd $RAW_DIR

mv ${ZIP_FILE} ${ZIP_FILE}.old

mv ${ZIP_FILE}.zip ${ZIP_FILE}

echo `date` > $CRC_FILE

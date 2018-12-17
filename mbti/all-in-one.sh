#/bin/sh

GLOVE_6B_URL="https://nlp.stanford.edu/data/wordvecs/glove.6B.zip"
GLOVE_6B_FILE_NAME="glove.6B.zip"
GLOVE_6B_MD5="373bba25d7289cf2b26837bfc428843a"
GLOVE_6B_DIR="glove6b"

INPUT_FILE="input/mbti_1.csv"
DATA_SEQ=512
EPOCH=10
EMBEDDING_DIM=300
DATA_FILE="data_seq_$DATA_SEQ.csv"
VOC_FILE="voc_seq_$DATA_SEQ.npy"

MODEL_DIR="models"
MODEL_PREFIX="model_seq_""$DATA_SEQ""_epoch_""$EPOCH""_embedding_""$EMBEDDING_DIM"


function download_and_unzip() {
    local data_dir=${1}
    local url=${2}
    local filename=${3}
    local sanity_md5=${4}
    local sub_dir=${5}

    echo ""
    echo "URL:            [$url]"
    echo "Filename:       [$filename]"
    echo "MD5:            [$sanity_md5]"
    echo "Sub-Directory:  [$sub_dir]"

    data_file_ready=false

    download_file=$data_dir/$filename
    if [ -f "$download_file" ]; then
        echo "Checking m5 of data file [$download_file] ..."

        if [ "$(uname)" == "Darwin" ]; then
            md5=`md5 -q $download_file`
        else
            md5=`md5sum $download_file | cut -f 1 -d " "`
        fi

    #    echo "md5 of [$download_file]: $md5"
    #    echo "md5 of Glove 6b: $sanity_md5"
        if [ $md5 == $sanity_md5 ]; then
            data_file_ready=true
        fi
    fi

    #echo "data file(s) ready: $data_file_ready"

    if [ "$data_file_ready" = false ]; then
        echo "Downloading required files to [$download_file] ..."
        wget -nd -c $url -O $download_file
    fi

    target_dir=$data_dir/$sub_dir
    if [ ! -d "$target_dir" ]; then
        echo "Creating sub data directory [$target_dir] ..."
        mkdir -p $target_dir
    fi

    if [ "$(uname)" == "Darwin" ]; then
      UNZIP="tar -C $target_dir/ -xf"
    else
      UNZIP="unzip -d $target_dir/ -nq"
    fi

    echo "Unpacking required data files into [$target_dir] ..."
    $UNZIP $download_file

    echo ""
}

function download_data() {
    local data_dir=${1}

    echo "STEP 1: Downloading data to [$data_dir] ..."
    download_and_unzip $data_dir $GLOVE_6B_URL $GLOVE_6B_FILE_NAME $GLOVE_6B_MD5 $GLOVE_6B_DIR
}

function pre_process_data() {
    local data_dir=${1}

    echo "STEP 2: Pre-processing data to [$data_dir] ..."
    python -u data_process.py -i input/mbti_1.csv -o $data_dir -m $DATA_SEQ
}

function train() {
    local data_dir=${1}

    echo "STEP 3: Training data from [$data_dir] ..."
    python -u train.py -d $data_dir/$DATA_FILE \
        -v $data_dir/$VOC_FILE -m $MODEL_DIR -g $data_dir/$GLOVE_6B_DIR \
        -ep $EPOCH -ed $EMBEDDING_DIM
}

function predict() {
    echo "STEP 4: Predicting with model(s) in [$MODEL_DIR], with prefix [$MODEL_PREFIX] ..."
    python -u predict.py -t "Come on!" -md $MODEL_DIR -mp $MODEL_PREFIX
}

if [ -z "$1" ]; then
  echo "usage $0 [data dir]"
  exit
fi

data_dir=$1
if [ ! -d "$data_dir" ]; then
    echo "Creating data directory [$data_dir] ..."
    mkdir -p $data_dir
fi

download_data $data_dir

pre_process_data $data_dir

train $data_dir

predict


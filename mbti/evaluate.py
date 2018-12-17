import os
import argparse

import numpy as np
import data_process
import tensorflow as tf
from tensorflow import keras

MODEL_FILE_NAME_TEMPLATE = 'model_{}_seq_{}_epoch_{}_embedding_{}.h5'


def predict_file(input_file, model_dir, model_prefix):
    print('predicting file: '.format(input_file))

    with open(input_file) as f:
        posts = f.readlines()
    print('{} post(s) in the file to be predicted'.format(len(posts)))

    return predict(posts, model_dir, model_prefix)


def predict(posts, model_dir, model_prefix):
    print('predicting: {} post(s), model = {}/{}_*.h5'.format(
        len(posts), model_dir, model_prefix))

    params = model_prefix.split('_')
    seq_max = int(params[2])
    print('seq_max: {}'.format(seq_max))

    posts_data, _ = data_process.process_posts_with_glove(posts, seq_max)
    print('post data: {}'.format(posts_data))

    predictions = []
    for i in range(0, 4):
        file = os.path.join(model_dir, model_prefix + '_' + str(i) + '.h5')
        print('model: {}'.format(file))
        model = keras.models.load_model(file)
        model.compile(optimizer=tf.train.AdamOptimizer(),
                      loss='binary_crossentropy',
                      metrics=['accuracy'])

        column_outputs = model.predict(posts_data)
        column_prediction = np.round(column_outputs).reshape(len(column_outputs)).astype(int)
        print('column predicted{}: {}'.format(i, column_prediction))
        predictions.append(column_prediction)

    outputs = np.asarray(predictions).T
    print('predictions: {}'.format(outputs))

    return outputs


def real_main():
    ap = argparse.ArgumentParser()

    input_group = ap.add_argument_group('input arguments')
    group = input_group.add_mutually_exclusive_group(required=True)
    group.add_argument("-f", "--input-file",
                       help="specify input data file")
    group.add_argument("-t", "--text",
                       help="specify text of posts")

    ap.add_argument("-md", "--model-dir", required=True,
                    help="specify directory of trained models")
    ap.add_argument("-mp", "--model-prefix", required=True,
                    help="specify prefix of trained models")

    args = ap.parse_args()

    if args.input_file is not None:
        outputs = predict_file(args.input_file, args.model_dir, args.model_prefix)
    else:
        outputs = predict([args.text], args.model_dir, args.model_prefix)

    print('results: {}'.format(data_process.translate_label_codes(outputs)))


if __name__ == "__main__":
    real_main()

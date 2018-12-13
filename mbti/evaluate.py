import os
import argparse
import timeit

import numpy as np
import data_process
import tensorflow as tf
from tensorflow import keras

DEFAULT_EPOCHS = 40
DEFAULT_BATCH_SIZE = 64
DEFAULT_TRAIN_FRACTION = .8
DEFAULT_EVAL_FRACTION = .2
DEFAULT_EMBEDDING_DIM = 300

MODEL_FILE_NAME_TEMPLATE = 'model_{}_seq_{}_epoch_{}_embedding_{}.h5'


def train_category(category,
                   train_x, train_y, test_x, test_y,
                   embedding_layers,
                   eval_split,
                   epoch, batch_size):
    start = timeit.default_timer()

    model = keras.Sequential()
    model.add(embedding_layers)

    # model.add(keras.layers.GlobalAveragePooling1D())
    model.add(keras.layers.Bidirectional(keras.layers.LSTM(64)))
    # model.add(keras.layers.Dense(16, activation=tf.nn.relu))
    model.add(keras.layers.Dense(1, activation=tf.nn.sigmoid))

    model.summary()

    model.compile(optimizer=tf.train.AdamOptimizer(),
                  loss='binary_crossentropy',
                  metrics=['accuracy'])

    history = model.fit(train_x,
                        train_y,
                        epochs=epoch,
                        batch_size=batch_size,
                        validation_split=eval_split,
                        shuffle=True,
                        verbose=1)

    results = model.evaluate(test_x, test_y)

    outputs = model.predict(test_x)
    predicted = np.round(outputs).reshape(len(outputs)).astype(int)
    print('MODEL: {}, outputs: {}'.format(category, outputs))
    print('MODEL: {}, predicted: {}'.format(category, predicted))

    end = timeit.default_timer()

    print('MODEL: {} is trained in {}s ({}s per epoch), results: {}'.format(
        category, round(end - start), round((end - start) / epoch), results))

    return model


def train_by_columns(data_file, voc_file, model_dir, glove_dir,
                     epoch, batch_size,
                     train_split, eval_split,
                     embedding_dim):
    print('training: data = {}, voc = {}, '
          'epochs = {}, batch size = {}, '
          'glove dir = {}, embedding dim = {} ---> models = {}'.format(data_file, voc_file,
                                                                       epoch, batch_size,
                                                                       glove_dir, embedding_dim,
                                                                       model_dir))

    train_x, train_y, test_x, test_y = data_process.load_data(
        data_file, train_split)
    vocab = data_process.load_vocab(voc_file)

    max_seq = train_x.shape[1]
    vocab_size = len(vocab) + 1
    print('training: sequence length = {}, vocabulary size = {}'.format(
        max_seq, vocab_size))

    embedding_layers = data_process.load_embeddings(
        vocab, max_seq, glove_dir, embedding_dim)

    for i in range(0, 4):
        model_name = 'model_{}'.format(i)
        model = train_category(model_name,
                               train_x, train_y.iloc[:, i],
                               test_x, test_y.iloc[:, i],
                               embedding_layers,
                               eval_split,
                               epoch, batch_size)

        model.save(os.path.join(model_dir,
                                MODEL_FILE_NAME_TEMPLATE.format(i, max_seq,
                                                                epoch, embedding_dim)))


def predict(input_file, model_dir, model_prefix):
    print('predicting: input = {}, model = {}/{}_*.h5'.format(
        input_file, model_dir, model_prefix))

    params = model_prefix.split('_')
    print('params: {}'.format(params))

    seq_max = int(params[2])
    print('seq_max: {}'.format(seq_max))

    with open(input_file) as f:
        posts = f.readlines()
    print('{} post to be predicted'.format(len(posts)))

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
        column_predication = np.round(column_outputs).reshape(len(column_outputs)).astype(int)
        print('column predicted{}: {}'.format(i, column_predication))
        predictions.append(column_predication)

    outputs = np.asarray(predictions).T
    print('predictions: {}'.format(outputs))

    return outputs


def real_main():
    ap = argparse.ArgumentParser()

    ap.add_argument("-i", "--input-file", required=True,
                    help="specify input data file")
    ap.add_argument("-md", "--model-dir", required=True,
                    help="specify directory of trained models")
    ap.add_argument("-mp", "--model-prefix", required=True,
                    help="specify prefix of trained models")

    args = ap.parse_args()

    outputs = predict(args.input_file, args.model_dir, args.model_prefix)

    print('results: {}'.format(data_process.translate_label_codes(outputs)))


if __name__ == "__main__":
    real_main()

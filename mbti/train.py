import os
import argparse
import timeit

import numpy as np
import data_process
import tensorflow as tf
from tensorflow import keras

DEFAULT_EPOCHS = 40
DEFAULT_BATCH_SIZE = 64
DEFAULT_SPLIT_FRACTION = .8
DEFAULT_EMBEDDING_DIM = 300

MODEL_FILE_NAME_TEMPLATE = 'model_{}_seq_{}_epoch_{}_embedding_{}.h5'


def train_category(category,
                   train_x, train_y, test_x, test_y,
                   embedding_layers,
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
                        validation_split=0.20,
                        shuffle=True,
                        verbose=1)

    results = model.evaluate(test_x, test_y)

    outputs = model.predict(test_x)
    predicted = np.argmax(outputs, axis=1)
    print('MODEL: {}, prediction: {}'.format(category, predicted))
    print(predicted)

    end = timeit.default_timer()

    print('MODEL: {} is trained in {}s ({}s per epoch), results: {}'.format(
        category, round(end - start), round((end - start) / epoch), results))

    return model


def train_by_columns(data_file, voc_file, model_dir, glove_dir,
                     epoch, batch_size, split_fraction, embedding_dim):
    print('training: data = {}, voc = {}, '
          'epochs = {}, batch size = {}, '
          'glove dir = {}, embedding dim = {} ---> models = {}'.format(data_file, voc_file,
                                                                       epoch, batch_size,
                                                                       glove_dir, embedding_dim,
                                                                       model_dir))

    train_x, train_y, test_x, test_y = data_process.load_data(
        data_file, split_fraction)
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
                               epoch, batch_size)

        model.save(os.path.join(model_dir,
                                MODEL_FILE_NAME_TEMPLATE.format(i, max_seq,
                                                                epoch, embedding_dim)))


def real_main():
    ap = argparse.ArgumentParser()

    io_group = ap.add_argument_group('input and output arguments')
    io_group.add_argument("-d", "--data-file", required=True,
                          help="specify pre-proceed MBTI data set file")
    io_group.add_argument("-v", "--voc-file", required=True,
                          help="specify vocabulary file")
    io_group.add_argument("-m", "--model-dir", required=True,
                          help="specify directory of output models")
    io_group.add_argument("-g", "--glove-dir", required=True,
                          help="specify directory of Glove")

    ml_group = ap.add_argument_group('learning parameters')
    ml_group.add_argument("-sf", "--split-fraction", required=False,
                          type=float,
                          default=DEFAULT_SPLIT_FRACTION,
                          help="specify split fraction of train set and test set")
    ml_group.add_argument("-ep", "--epoch", required=False,
                          type=int,
                          default=DEFAULT_EPOCHS,
                          help="specify epoch count")
    ml_group.add_argument("-bs", "--batch-size", required=False,
                          type=int,
                          default=DEFAULT_BATCH_SIZE,
                          help="specify batch size in each epoch")
    ml_group.add_argument("-ed", "--embedding-dim", required=False,
                          type=int,
                          choices=[50, 100, 200, 300],
                          default=DEFAULT_EMBEDDING_DIM,
                          help="specify batch size in each epoch")

    args = ap.parse_args()

    if not os.path.isdir(args.model_dir):
        os.mkdir(args.model_dir)

    train_by_columns(args.data_file,
                     args.voc_file,
                     args.model_dir,
                     args.glove_dir,
                     args.epoch,
                     args.batch_size,
                     args.split_fraction,
                     args.embedding_dim)


if __name__ == "__main__":
    real_main()

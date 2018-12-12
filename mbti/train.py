import os
import numpy as np
import pandas as pd
import data_process
import tensorflow as tf
from tensorflow import keras

INPUT_FILE = './input/mbti_1.csv'
VOC_FILE = 'data/voc_int.npy'
DATA_FILE = 'data/data.csv'

MAX_SEQUENCE_LENGTH = 512
MAX_FEATURES = 2000

GLOVE_DIR = 'data/glove6b'
EMBEDDING_DIM = 300
MAX_NB_WORDS = 2000


def pre_process_data(file, seq_max=0):
    text = pd.read_csv(file, index_col='type')

    raw_labels = text.index.tolist()
    raw_posts = text.posts.tolist()

    train_data, vocab_data = data_process.pre_process_with_glove(raw_labels, raw_posts, seq_max)

    print('saving the vocabulary into file: [{}]'.format(VOC_FILE))
    np.save(VOC_FILE, vocab_data)

    print('saving the data into file: [{}] '.format(DATA_FILE))
    df = pd.DataFrame(train_data)
    df.to_csv(DATA_FILE)


def load_data(file, split_frac=0.8):
    df = pd.read_csv(file)
    print('[DATA SET]')
    print(df.head(5))
    print()

    rows = df.shape[0]
    print('{} records'.format(rows))

    train_rows = int(split_frac * rows)
    # remained_rows = rows - train_rows
    # eval_rows = int(remained_rows / 2)
    test_rows = rows - train_rows

    print('{} train records'.format(train_rows))
    #    print('{} eval records'.format(eval_rows))
    print('{} test records'.format(test_rows))

    # eval_start = train_rows
    test_start = train_rows

    train_features = df.loc[:train_rows, '4':]
    train_labels = df.loc[:train_rows, '0':'3']
    print('train features: \n{}'.format(train_features.head(5)))
    print('train labels: \n{}'.format(train_labels.head(5)))

    # eval_features = df.loc[eval_start:train_rows + eval_rows, '5':]
    # eval_labels = df.loc[eval_start:train_rows + eval_rows, '0':'4']
    # print('eval features: \n{}'.format(train_features.head(5)))
    # print('eval labels: \n{}'.format(train_labels.head(5)))

    test_features = df.loc[test_start:, '4':]
    test_labels = df.loc[test_start:, '0':'3']
    print('test features: \n{}'.format(test_features.head(5)))
    print('test labels: \n{}'.format(test_labels.head(5)))

    # return train_features, train_labels, eval_features, eval_labels, test_features, test_labels
    return train_features, train_labels, test_features, test_labels


def load_vocab(dict_file):
    vocab_data = np.load(dict_file).item()
    return vocab_data


def load_embeddings(vocab, dim=EMBEDDING_DIM):
    embeddings_index = {}
    f = open(os.path.join(GLOVE_DIR, 'glove.6B.%sd.txt' % str(dim)))
    for line in f:
        values = line.split()
        word = values[0]
        coefs = np.asarray(values[1:], dtype='float32')
        embeddings_index[word] = coefs
    f.close()

    print('Found %s word vectors.' % len(embeddings_index))

    # prepare embedding matrix
    num_words = min(MAX_NB_WORDS, len(vocab))
    embedding_matrix = np.zeros((num_words, EMBEDDING_DIM))
    for word, i in vocab.items():
        if i >= MAX_NB_WORDS:
            continue
        embedding_vector = embeddings_index.get(word)
        if embedding_vector is not None:
            # words not found in embedding index will be all-zeros.
            embedding_matrix[i] = embedding_vector

    # load pre-trained word embeddings into an Embedding layer
    embedding_layer = keras.layers.Embedding(num_words,
                                             EMBEDDING_DIM,
                                             weights=[embedding_matrix],
                                             input_length=MAX_SEQUENCE_LENGTH,
                                             trainable=False)

    return embedding_layer


def train():
    # train_x, train_y, eval_x, eval_y, test_x, test_y = load_data(DATA_FILE)
    train_x, train_y, test_x, test_y = load_data(DATA_FILE)

    vocab = load_vocab(VOC_FILE)
    vocab_size = len(vocab) + 1

    print('Vocabulary size: {}'.format(vocab_size))

    embedding_layers = load_embeddings(vocab)

    model = keras.Sequential()
    model.add(embedding_layers)
    # model.add(keras.layers.GlobalAveragePooling1D())
    model.add(keras.layers.Bidirectional(keras.layers.LSTM(64)))
    # model.add(keras.layers.Dense(128, activation='relu'))
    model.add(keras.layers.Dense(4))

    model.summary()

    model.compile(optimizer=tf.train.AdamOptimizer(),
                  loss='mse',
                  metrics=['accuracy'])

    history = model.fit(train_x,
                        train_y,
                        epochs=80,
                        batch_size=64,
                        validation_split=0.20,
                        shuffle=True,
                        verbose=1)

    results = model.evaluate(test_x, test_y)
    print(results)

    print(test_x.head(5))
    print(test_y.head(5))
    outputs = model.predict(test_x)
    predicted = np.argmax(outputs, axis=1)
    print(predicted)


def train_category(category, train_x, train_y, test_x, test_y, vocab, embedding_layers):
    model = keras.Sequential()
    model.add(embedding_layers)
    # model.add(keras.layers.GlobalAveragePooling1D())
    model.add(keras.layers.Bidirectional(keras.layers.LSTM(64)))
    # model.add(keras.layers.Dense(128, activation='relu'))
    model.add(keras.layers.Dense(1, activation=tf.nn.sigmoid))

    model.summary()

    model.compile(optimizer=tf.train.AdamOptimizer(),
                  loss='binary_crossentropy',
                  metrics=['accuracy'])

    history = model.fit(train_x,
                        train_y,
                        epochs=10,
                        batch_size=64,
                        validation_split=0.20,
                        shuffle=True,
                        verbose=1)

    results = model.evaluate(test_x, test_y)
    print('CATEGORY: {}, results: {}'.format(category, results))

    outputs = model.predict(test_x)
    predicted = np.argmax(outputs, axis=1)
    print('CATEGORY: {}, prediction: {}'.format(category, predicted))
    print(predicted)


def train_4d():
    # train_x, train_y, eval_x, eval_y, test_x, test_y = load_data(DATA_FILE)
    train_x, train_y, test_x, test_y = load_data(DATA_FILE)

    vocab = load_vocab(VOC_FILE)
    vocab_size = len(vocab) + 1

    print('Vocabulary size: {}'.format(vocab_size))

    embedding_layers = load_embeddings(vocab)

    for i in range(0, 4):
        train_category('category_0',
                       train_x, train_y.iloc[:, i],
                       test_x, test_y.iloc[:, i],
                       vocab, embedding_layers)


# pre_process_data(INPUT_FILE, MAX_SEQUENCE_LENGTH)

# train()

train_4d()

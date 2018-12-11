import numpy as np
import pandas as pd
import data_process
import tensorflow as tf
from tensorflow import keras

INPUT_FILE = './input/mbti_1.csv'
VOC_FILE = 'data/voc_int.npy'
DATA_FILE = 'data/data.csv'

MAX_SEQUENCE_LENGTH = 923
MAX_FEATURES = 2000


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


def train():

    # train_x, train_y, eval_x, eval_y, test_x, test_y = load_data(DATA_FILE)
    train_x, train_y, test_x, test_y = load_data(DATA_FILE)

    vocab = load_vocab(VOC_FILE)
    vocab_size = len(vocab) + 1

    print('Vocabulary size: {}'.format(vocab_size))

    model = keras.Sequential()
    model.add(keras.layers.Embedding(MAX_FEATURES, 256, input_length=MAX_SEQUENCE_LENGTH))
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
                        epochs=40,
                        batch_size=32,
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

# pre_process_data(INPUT_FILE, MAX_SEQUENCE_LENGTH)

train()
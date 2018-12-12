import numpy as np
import data_process
import tensorflow as tf
from tensorflow import keras

INPUT_FILE = './input/mbti_1.csv'
VOC_FILE = 'data/voc_int.npy'
DATA_FILE = 'data/data.csv'

MODEL_DIR = 'models'

MAX_SEQUENCE_LENGTH = 512
MAX_FEATURES = 2000

GLOVE_DIR = 'data/glove6b'
EMBEDDING_DIM = 300
MAX_NB_WORDS = 2000


def train():
    # train_x, train_y, eval_x, eval_y, test_x, test_y = load_data(DATA_FILE)
    train_x, train_y, test_x, test_y = data_process.load_data(DATA_FILE)

    vocab = data_process.load_vocab(VOC_FILE)
    vocab_size = len(vocab) + 1

    print('Vocabulary size: {}'.format(vocab_size))

    embedding_layers = data_process.load_embeddings(vocab, MAX_SEQUENCE_LENGTH, EMBEDDING_DIM)

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


def train_category(category,
                   train_x, train_y, test_x, test_y,
                   embedding_layers,
                   epoch, batch_size):
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
                        epochs=epoch,
                        batch_size=batch_size,
                        validation_split=0.20,
                        shuffle=True,
                        verbose=1)

    results = model.evaluate(test_x, test_y)
    print('MODEL: {}, results: {}'.format(category, results))

    outputs = model.predict(test_x)
    predicted = np.argmax(outputs, axis=1)
    print('MODEL: {}, prediction: {}'.format(category, predicted))
    print(predicted)

    return model


def train_4d(epoch, batch_size):
    # train_x, train_y, eval_x, eval_y, test_x, test_y = load_data(DATA_FILE)
    train_x, train_y, test_x, test_y = data_process.load_data(DATA_FILE)

    vocab = data_process.load_vocab(VOC_FILE)
    vocab_size = len(vocab) + 1

    print('Vocabulary size: {}'.format(vocab_size))

    embedding_layers = data_process.load_embeddings(
        vocab, MAX_SEQUENCE_LENGTH, EMBEDDING_DIM)

    for i in range(0, 4):
        model_name = 'model_{}'.format(i)
        model = train_category(model_name,
                               train_x, train_y.iloc[:, i],
                               test_x, test_y.iloc[:, i],
                               embedding_layers,
                               epoch, batch_size)

        model.save('{}/mbti_{}.h5'.format(MODEL_DIR, model_name))


# data_process.pre_process_data(INPUT_FILE,
#                               DATA_FILE,
#                               VOC_FILE,
#                               MAX_SEQUENCE_LENGTH)

# train()

epoch = 1
batch_size = 256
train_4d(epoch, batch_size)

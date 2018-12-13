import os
import argparse
import numpy as np
import pandas as pd
from keras_preprocessing.sequence import pad_sequences
from keras_preprocessing.text import Tokenizer
from tensorflow import keras
import utils

MAX_NB_WORDS = 2000
DEFAULT_MAX_SEQ = 512

DATA_FILE_NAME_TEMPLATE = 'data_seq_{}.csv'
VOC_FILE_NAME_TEMPLATE = 'voc_seq_{}.npy'


class LabelTranslator:
    titles = ["Extroversion (E) - Introversion (I)",
              "Sensation (S) - Nutation (N)",
              "Thinking (T) - Feeling (F)",
              "Judgement (J) - Perception (P)"
              ]

    b_Pers = {'I': 0, 'E': 1, 'N': 0, 'S': 1, 'F': 0, 'T': 1, 'J': 0, 'P': 1}
    b_Pers_list = [{0: 'I', 1: 'E'}, {0: 'N', 1: 'S'}, {0: 'F', 1: 'T'}, {0: 'J', 1: 'P'}]

    def encode(self, personality):
        """
        transform mbti to binary vector
        """
        return [self.b_Pers[l] for l in personality]

    def decode(self, personality):
        """
        transform binary vector to mbti personality
        """
        s = ""
        for i, l in enumerate(personality):
            s += self.b_Pers_list[i][l]
        return s

    def encode_all(self, personalities):
        codes = []
        for p in personalities:
            codes.append(self.encode(p))

        return np.asarray(codes)

    def decode_all(self, codes):
        personalities = []
        for c in codes:
            personalities.append(self.decode(c))

        return personalities


def process_labels(labels):
    encoder = LabelTranslator()
    return encoder.encode_all(labels)


def translate_label_codes(codes):
    encoder = LabelTranslator()
    return encoder.decode_all(codes)


def process_posts_with_glove(posts, max_seq_len=DEFAULT_MAX_SEQ):
    posts_text = [utils.post_cleaner(post) for post in posts]

    tokenizer = Tokenizer(num_words=MAX_NB_WORDS)
    tokenizer.fit_on_texts(posts_text)
    sequences = tokenizer.texts_to_sequences(posts_text)

    word_index = tokenizer.word_index
    print('Found %s unique tokens.' % len(word_index))

    data = pad_sequences(sequences, maxlen=max_seq_len, padding='post', truncating='post')

    return data, word_index


def pre_process_with_glove(labels, posts, seq_max=0):
    labels_data = process_labels(labels)
    posts_data, vocab_data = process_posts_with_glove(posts, seq_max)

    print('posts_data: ' + str(posts_data.shape))
    print(posts_data[0])

    data = np.concatenate((labels_data, posts_data), axis=1)
    print('data[0]: ' + str(data[10:]))

    return data, vocab_data


def pre_process_data(input_data_file, output_data_dir, seq_max):
    data_file = os.path.join(output_data_dir, DATA_FILE_NAME_TEMPLATE.format(seq_max))
    voc_file = os.path.join(output_data_dir, VOC_FILE_NAME_TEMPLATE.format(seq_max))

    print('data processing: input = {} --> data = {}, vocab = {}, sequence length = {}'.format(
        input_data_file, data_file, voc_file, seq_max))
    text = pd.read_csv(input_data_file, index_col='type')

    raw_labels = text.index.tolist()
    raw_posts = text.posts.tolist()

    train_data, vocab_data = pre_process_with_glove(raw_labels, raw_posts, seq_max)

    print('saving the vocabulary into file: [{}]'.format(voc_file))
    np.save(voc_file, vocab_data)

    print('saving the data into file: [{}] '.format(data_file))
    df = pd.DataFrame(train_data)
    df.to_csv(data_file)


def load_data(data_file, split_fraction=0.8):
    df = pd.read_csv(data_file)
    print('[DATA SET]')
    print(df.head(5))
    print()

    rows = df.shape[0]
    print('{} records'.format(rows))

    train_rows = int(split_fraction * rows)
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


def load_vocab(vocab_file):
    vocab_data = np.load(vocab_file).item()
    return vocab_data


def load_embeddings(vocab, max_seq, glove_dir, dim):
    embeddings_index = {}
    f = open(os.path.join(glove_dir, 'glove.6B.%sd.txt' % str(dim)))
    for line in f:
        values = line.split()
        word = values[0]
        coefs = np.asarray(values[1:], dtype='float32')
        embeddings_index[word] = coefs
    f.close()

    print('Found %s word vectors.' % len(embeddings_index))

    # prepare embedding matrix
    num_words = min(MAX_NB_WORDS, len(vocab))
    embedding_matrix = np.zeros((num_words, dim))
    for word, i in vocab.items():
        if i >= MAX_NB_WORDS:
            continue
        embedding_vector = embeddings_index.get(word)
        if embedding_vector is not None:
            # words not found in embedding index will be all-zeros.
            embedding_matrix[i] = embedding_vector

    # load pre-trained word embeddings into an Embedding layer
    embedding_layer = keras.layers.Embedding(num_words,
                                             dim,
                                             weights=[embedding_matrix],
                                             input_length=max_seq,
                                             trainable=False)

    return embedding_layer


def real_main():
    ap = argparse.ArgumentParser()

    ap.add_argument("-i", "--input-file", required=True,
                    help="specify input original MBTI data set file")
    ap.add_argument("-o", "--data-dir", required=True,
                    help="specify output data directory")
    ap.add_argument("-m", "--max-seq", required=False,
                    type=int,
                    default=[DEFAULT_MAX_SEQ],
                    nargs='+',
                    choices=[128, 256, 512, 900],
                    help="specify maximum sequence length")

    args = ap.parse_args()

    if not os.path.isdir(args.data_dir):
        os.mkdir(args.data_dir)

    max_seq = list(set(args.max_seq))
    print('max sequence(s): {}'.format(max_seq))
    for seq in max_seq:
        pre_process_data(args.input_file,
                         args.data_dir,
                         seq)


if __name__ == "__main__":
    real_main()

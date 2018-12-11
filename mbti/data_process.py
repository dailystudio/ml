import numpy as np
import pandas as pd
from keras_preprocessing.sequence import pad_sequences
from keras_preprocessing.text import Tokenizer

import utils
from sklearn.preprocessing import LabelBinarizer
from collections import Counter

MAX_NB_WORDS = 2000
DEFAULT_MAX_SEQ = 923


class LabelTranslator:
    titles = ["Extraversion (E) - Introversion (I)",
              "Sensation (S) - INtuition (N)",
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


class LabelEncoder:
    MBTI_LABELS = {0: 'ENFJ', 1: 'ENFP', 2: 'ENTJ', 3: 'ENTP',
                   4: 'ESFJ', 5: 'ESFP', 6: 'ESTJ', 7: 'ESTP',
                   8: 'INFJ', 9: 'INFP', 10: 'INTJ', 11: 'INTP',
                   12: 'ISFJ', 13: 'ISFP', 14: 'ISTJ', 15: 'ISTP'}

    encoder = None

    def __init__(self):
        self.encoder = LabelBinarizer(neg_label=0, pos_label=1, sparse_output=False)

        labels = []
        for l in self.MBTI_LABELS.values():
            labels.append(l)
        self.encoder.fit(labels)

    def encode(self, labels):
        labels_data = np.array(self.encoder.transform(labels))

        rev_labels = self.decode(labels_data)
        for i in range(0, len(labels)):
            print('[LABEL] raw: %s, token: %s, rev: %s' % (labels[i], labels_data[i], rev_labels[i]))

        return labels_data

    def decode(self, labels_data):
        return self.encoder.inverse_transform(labels_data)


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
    print('sequences: {}'.format(sequences))
    print('index: {}'.format(word_index))
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


def process_posts(posts):
    posts_text = [utils.post_cleaner(post) for post in posts]

    word_count = Counter()
    for post in posts_text:
        word_count.update(post.split(" "))

    vocab_len = len(word_count)
    print(vocab_len)

    # Create a look up table
    vocab_list = sorted(word_count, key=word_count.get, reverse=True)
    # Create your dictionary that maps vocab words to integers here
    vocab_data = {word: ii for ii, word in enumerate(vocab_list, 1)}

    posts_ints = []
    for post in posts_text:
        posts_ints.append([vocab_data[word] for word in post.split()])

    print('[POST] raw: [%s]' % (posts[0]))
    print('------------------------------------------------')
    print('[POST] cleaned up: [%s]' % (posts_text[0]))
    print('------------------------------------------------')
    print('[POST] ints: [%s]' % (posts_ints[0]))
    print('------------------------------------------------')

    print('posts_text: ' + str(len(posts_text)))
    print('posts_ints: ' + str(len(posts_ints)))

    posts_data = np.asarray(posts_ints)
    print('posts_data: ' + str(posts_data.shape))

    return posts_ints, vocab_data


def pre_process(labels, posts, seq_max=0):
    labels_data = process_labels(labels)
    posts_data, vocab_data = process_posts(posts, seq_max)

    posts_data_lens = [len(x) for x in posts_data]
    average_len = int(np.mean(posts_data_lens))

    print("Maximum post ints length: {}".format(max(posts_data_lens)))
    print("Minimum post ints length: {}".format(min(posts_data_lens)))
    print("Average post ints length: {}".format(average_len))

    if seq_max > 0:
        seq_len = seq_max
    else:
        seq_len = int(average_len)

    features = np.zeros((len(posts_data), seq_len), dtype=int)
    for i, row in enumerate(posts_data):
        features[i, 0: len(row)] = np.array(row)[:seq_len]
    print('features: ' + str(features.shape))
    print(features[0])

    data = np.concatenate((labels_data, features), axis=1)
    print('data[0]: ' + str(data[10:]))

    return data, vocab_data

INPUT_FILE = './input/mbti_1.csv'


def __test_labels_encoding__():

    text = pd.read_csv(INPUT_FILE, index_col='type')

    raw_labels = text.index.tolist()
    codes = process_labels(raw_labels)
    personalities = translate_label_codes(codes)

    for i in range(0, len(raw_labels)):
        print('raw: {}, code: {}, personality: {}'.format(raw_labels[i], codes[i], personalities[i]))

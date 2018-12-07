import pandas as pd
import numpy as np
import utils
from sklearn.preprocessing import LabelBinarizer
from collections import Counter

VOC_FILE = 'data/voc_int.npy'
DATA_FILE = 'data/data.csv'

mbti_dict = {0: 'ENFJ', 1: 'ENFP', 2: 'ENTJ', 3: 'ENTP', 4: 'ESFJ', 5: 'ESFP', 6: 'ESTJ', 7: 'ESTP', 8: 'INFJ',
             9: 'INFP', 10: 'INTJ', 11: 'INTP', 12: 'ISFJ', 13: 'ISFP', 14: 'ISFP', 15: 'ISTP'}

text = pd.read_csv("./input/mbti_1.csv", index_col='type')

raw_labels = text.index.tolist()
encoder = LabelBinarizer(neg_label=0, pos_label=1, sparse_output=False)
labels = encoder.fit_transform(raw_labels)
labels = np.array(labels)

rev_labels = encoder.inverse_transform(labels)
print('[LABEL] raw: %s, token: %s, rev: %s' % (raw_labels[0], labels[0], rev_labels[0]))

raw_posts = text.posts.tolist()
posts = [utils.post_cleaner(post) for post in raw_posts]

# Count total words

word_count = Counter()
for post in posts:
    word_count.update(post.split(" "))

vocab_len = len(word_count)
print(vocab_len)

print('saving the vocabulary into file: ' + VOC_FILE)
# Create a look up table
vocab = sorted(word_count, key=word_count.get, reverse=True)
# Create your dictionary that maps vocab words to integers here
vocab_to_int = {word: ii for ii, word in enumerate(vocab, 1)}
np.save(VOC_FILE, vocab_to_int)

posts_ints = []
for post in posts:
    posts_ints.append([vocab_to_int[word] for word in post.split()])

print('[POST] raw: [%s]' % (raw_posts[0]))
print('------------------------------------------------')
print('[POST] cleaned up: [%s]' % (posts[0]))
print('------------------------------------------------')
print('[POST] ints: [%s]' % (posts_ints[0]))
print('------------------------------------------------')

print('labels: ' + str(labels.shape))
print('posts: ' + str(len(posts)))
print('posts_ints: ' + str(len(posts_ints)))

posts_array = np.asarray(posts_ints)
print('posts_array: ' + str(posts_array.shape))

posts_ints_lens = [len(x) for x in posts_array]
average_len = int(np.mean(posts_ints_lens))

print("Maximum post ints length: {}".format(max(posts_ints_lens)))
print("Minimum post ints length: {}".format(min(posts_ints_lens)))
print("Average post ints length: {}".format(average_len))

seq_len = int(average_len)
features = np.zeros((len(posts_ints), seq_len), dtype=int)
for i, row in enumerate(posts_ints):
    features[i, 0: len(row)] = np.array(row)[:seq_len]
print('features: ' + str(features.shape))
print(features[0])


data = np.concatenate((labels, features), axis=1)
print('data[0]: ' + str(data[10:]))

df = pd.DataFrame(data)
df.to_csv(DATA_FILE)

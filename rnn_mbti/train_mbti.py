# coding: utf-8

# In[ ]:

# This Python 3 environment comes with many helpful analytics libraries installed
# It is defined by the kaggle/python docker image: https://github.com/kaggle/docker-python
# For example, here's several helpful packages to load in 

import numpy as np
import pandas as pd
import tensorflow as tf
import pickle
import utils

# Input data files are available in the "../input/" directory.
# For example, running this (by clicking run or pressing Shift+Enter) will list the files in the input directory

from subprocess import check_output

VOC_FILE = 'data/voc_int.npy'

print(check_output(["ls", "./input"]).decode("utf8"))

# ## Load dataset
# 
# The dataset is a 'csv' file, so we'll use pandas to load it. We shall print the shape and the first few entries of
# the dataset to understand what we're working with. Accordingly, we need to choose what strategy to use to clean the
#  data.

# In[ ]:

# load dataset
text = pd.read_csv("./input/mbti_1.csv", index_col='type')
print(text.shape)
print(text[0:5])
print(text.iloc[2])

# ## Preprocessing labels The neural letwork cannot understand string labels, so we one-hot-encode them using
# sklearn.preprocessing.LabelBinarizer. I'm displaying the first few labels to see if everything's okay.

# In[ ]:

from sklearn.preprocessing import LabelBinarizer

# One hot encode labels
labels = text.index.tolist()
encoder = LabelBinarizer(neg_label=0, pos_label=1, sparse_output=False)
labels = encoder.fit_transform(labels)
labels = np.array(labels)
print(labels[50:55])

# In[ ]:

mbti_dict = {0: 'ENFJ', 1: 'ENFP', 2: 'ENTJ', 3: 'ENTP', 4: 'ESFJ', 5: 'ESFP', 6: 'ESTJ', 7: 'ESTP', 8: 'INFJ',
             9: 'INFP', 10: 'INTJ', 11: 'INTP', 12: 'ISFJ', 13: 'ISFP', 14: 'ISFP', 15: 'ISTP'}

# ### Preprocessing posts
# 
# We can see that the posts are very noisy, so they need to be cleaned. For this I'm doing the following:
# 
# 1. Converting all letters to lowercase.
# 2. Remove '|||'
# 3. Removing punctuation.
# 4. Removing URLs, links etc..
# 5. Convert words to integers
# 
# We'll leave unicode emojis alone.

# In[ ]:

# In[ ]:

# Clean up posts
# Covert pandas dataframe object to list. I prefer using lists for prepocessing. 
posts = text.posts.tolist()
posts = [utils.post_cleaner(post) for post in posts]

# In[ ]:

# Count total words
from collections import Counter

word_count = Counter()
for post in posts:
    word_count.update(post.split(" "))

# In[ ]:

# Size of the vocabulary available to the RNN
vocab_len = len(word_count)
print(vocab_len)

print(len(posts[0]))

# ### Convert words to integers

# In[ ]:

# Create a look up table 
vocab = sorted(word_count, key=word_count.get, reverse=True)
# Create your dictionary that maps vocab words to integers here
vocab_to_int = {word: ii for ii, word in enumerate(vocab, 1)}

print('saving the vocabulary to file: ' + VOC_FILE)
np.save(VOC_FILE, vocab_to_int)

posts_ints = []
for post in posts:
    posts_ints.append([vocab_to_int[word] for word in post.split()])

print(posts_ints[0])
print(len(posts_ints[0]))

# ### Make posts uniform We can see that the lengths of the posts aren't uniform, so we'll limit number of words in
# each post to 1000.For posts with less than 1000 words, we'll pad with zeros.

# In[ ]:

posts_lens = Counter([len(x) for x in posts])
print("Zero-length reviews: {}".format(posts_lens[0]))
print("Maximum review length: {}".format(max(posts_lens)))
print("Minimum review length: {}".format(min(posts_lens)))

seq_len = 500
features = np.zeros((len(posts_ints), seq_len), dtype=int)
for i, row in enumerate(posts_ints):
    features[i, -len(row):] = np.array(row)[:seq_len]
print(features[:10])

# ### Preparing tranining, test and validation datasets

# In[ ]:

# Split data into training, test and validation

split_frac = 0.8

num_ele = int(split_frac * len(features))
rem_ele = len(features) - num_ele
train_x, val_x = features[:num_ele], features[num_ele:int(rem_ele / 2) + num_ele]
train_y, val_y = labels[:num_ele], labels[num_ele:int(rem_ele / 2) + num_ele]

test_x = features[num_ele + int(rem_ele / 2):]
test_y = labels[num_ele + int(rem_ele / 2):]

print("\t\t\tFeature Shapes:")
print("Train set: \t\t{}".format(train_x.shape),
      "\nValidation set: \t{}".format(val_x.shape),
      "\nTest set: \t\t{}".format(test_x.shape))

# ## The RNN

# In[ ]:

lstm_size = 256
lstm_layers = 1
batch_size = 256
learning_rate = 0.01
embed_dim = 250

# In[ ]:

n_words = len(vocab_to_int) + 1  # Adding 1 because we use 0's for padding, dictionary started at 1

# Create the graph object
graph = tf.Graph()
# Add nodes to the graph
with graph.as_default():
    input_data = tf.placeholder(tf.int32, [None, None], name='inputs')
    labels_ = tf.placeholder(tf.int32, [None, None], name='labels')
    keep_prob = tf.placeholder(tf.float32, name='keep_prob')

# In[ ]:

# Embedding
with graph.as_default():
    embedding = tf.Variable(tf.random_uniform(shape=(n_words, embed_dim), minval=-1, maxval=1))
    embed = tf.nn.embedding_lookup(embedding, input_data)
    print(embed.shape)

# In[ ]:

# LSTM cell
with graph.as_default():
    # basic LSTM cell
    lstm = tf.contrib.rnn.BasicLSTMCell(lstm_size)

    # Add dropout to the cell
    drop = tf.contrib.rnn.DropoutWrapper(lstm, output_keep_prob=keep_prob)

    # Stack up multiple LSTM layers, for deep learning
    cell = tf.contrib.rnn.MultiRNNCell([drop] * lstm_layers)

    # Getting an initial state of all zeros
    initial_state = cell.zero_state(batch_size, tf.float32)

# In[ ]:

with graph.as_default():
    outputs, final_state = tf.nn.dynamic_rnn(cell, embed, dtype=tf.float32)

# In[ ]:

with graph.as_default():
    pre = tf.layers.dense(outputs[:, -1], 16, activation=tf.nn.relu)
    predictions = tf.layers.dense(pre, 16, activation=tf.nn.softmax)

    cost = tf.losses.mean_squared_error(labels_, predictions)
    optimizer = tf.train.AdamOptimizer(learning_rate).minimize(cost)

# In[ ]:

with graph.as_default():
    correct_pred = tf.equal(tf.cast(tf.round(predictions), tf.int32), labels_)
    accuracy = tf.reduce_mean(tf.cast(correct_pred, tf.float32))


# In[ ]:

def get_batches(x, y, batch_size=100):
    n_batches = len(x) // batch_size
    x, y = x[:n_batches * batch_size], y[:n_batches * batch_size]
    for ii in range(0, len(x), batch_size):
        yield x[ii:ii + batch_size], y[ii:ii + batch_size]


# ## Training

# In[ ]:

epochs = 3

with graph.as_default():
    saver = tf.train.Saver()

with tf.Session(graph=graph) as sess:
    sess.run(tf.global_variables_initializer())
    iteration = 1
    for e in range(epochs):
        state = sess.run(initial_state)

        for ii, (x, y) in enumerate(get_batches(train_x, train_y, batch_size), 1):
            feed = {input_data: x,
                    labels_: y,
                    keep_prob: 1.0,
                    initial_state: state}
            loss, state, _ = sess.run([cost, final_state, optimizer], feed_dict=feed)

            if iteration % 5 == 0:
                print("Epoch: {}/{}".format(e, epochs),
                      "Iteration: {}".format(iteration),
                      "Train loss: {:.3f}".format(loss))

            if iteration % 25 == 0:
                val_acc = []
                val_state = sess.run(cell.zero_state(batch_size, tf.float32))
                for x, y in get_batches(val_x, val_y, batch_size):
                    feed = {input_data: x,
                            labels_: y,
                            keep_prob: 1,
                            initial_state: val_state}
                    batch_acc, val_state = sess.run([accuracy, final_state], feed_dict=feed)
                    val_acc.append(batch_acc)
                print("Val acc: {:.3f}".format(np.mean(val_acc)))
            iteration += 1
    saver.save(sess, "checkpoints/mbti.ckpt")

# ## Testing

# In[ ]:

test_acc = []
with tf.Session(graph=graph) as sess:
    saver.restore(sess, tf.train.latest_checkpoint('checkpoints'))
    test_state = sess.run(cell.zero_state(batch_size, tf.float32))
    for ii, (x, y) in enumerate(get_batches(test_x, test_y, batch_size), 1):
        feed = {input_data: x,
                labels_: y,
                keep_prob: 1,
                initial_state: test_state}
        batch_acc, test_state = sess.run([accuracy, final_state], feed_dict=feed)
        test_acc.append(batch_acc)
    print("Test accuracy: {:.3f}".format(np.mean(test_acc)))

# In[ ]:

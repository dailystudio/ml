import numpy as np

a = np.asarray([[1, 2], [1, 2, 3], [3, 3, 3], [4, 4, 3, 5], [8, 8, 10, 12, 12]])

lens = [len(x) for x in a]
# avg_len = int(np.mean(lens))
avg_len = 5

print("Maximum review length: {}".format(max(lens)))
print("Minimum review length: {}".format(min(lens)))
print("Average review length: {}".format(avg_len))

print(lens)

features = np.zeros((len(a), avg_len), dtype=int)
for i, row in enumerate(a):
    print('i: {}, row: {}'.format(i, row))
    features[i, 0: len(row)] = np.array(row)[:avg_len]
print(features)




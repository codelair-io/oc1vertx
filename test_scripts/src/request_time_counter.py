import csv

line_to_read = 0
number_of_columns = 20
input_files = {('Quarkus', '../../data/qk_prime_load_test_Linux_x86_64.txt'),
               ('KumuluzEE', '../../data/ke_prime_load_test_Linux_x86_64.txt'),
               ('SpringBoot', '../../data/sb_prime_load_test_Linux_x86_64.txt'),
               ('VertX', '../../data/vx_prime_load_test_Linux_x86_64.txt')}
step_size = 10000
start = 000
final = []
for input_file in input_files:
  with open(input_file[1], 'r') as f:
    reader = csv.reader(f.readlines())
    values = list(map(int, list(reader)[line_to_read]))
    row = [input_file[0]]
    for step in range(start, step_size * number_of_columns, step_size):
      row.append(len(list(x for x in values if step <= x <= step + step_size - 1)))
    final.append(','.join(map(str, row)))

print(',' + ','.join(
  map(str, range(int(start / 1000), int(step_size / 1000) * number_of_columns, int(step_size / 1000)))))
for s in final:
  print(s)

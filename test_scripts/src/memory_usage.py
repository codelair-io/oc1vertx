import csv

line_to_read = 3
number_of_columns = 20
input_files = {('Quarkus', '../../data/qk_prime_load_test_Linux_x86_64.txt'),
               ('KumuluzEE', '../../data/ke_prime_load_test_Linux_x86_64.txt'),
               ('SpringBoot', '../../data/sb_prime_load_test_Linux_x86_64.txt'),
               ('VertX', '../../data/vx_prime_load_test_Linux_x86_64.txt')}
step_size = 10
start = 000
final = []
for input_file in input_files:
  with open(input_file[1], 'r') as f:
    reader = csv.reader(f.readlines())
    values = [input_file[0]]
    values.extend(list(map(int, list(reader)[line_to_read])))
    final.append(','.join(map(str, values[0::step_size])))

print('Memory usage')
for s in final:
  print(s)

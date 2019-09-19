import csv

line_to_read = 0
number_of_columns = 50
input_file = 'data/vx_serial_Linux_armv7l.txt'

with open(input_file, 'r') as f:
  reader = csv.reader(f.readlines())
  values = list(map(int, list(reader)[line_to_read]))

print(input_file)
print(','.join(map(str, values[:number_of_columns])))

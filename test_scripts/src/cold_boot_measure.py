import json
import platform
import random
import requests
import string
import subprocess
import sys
import time
from json import JSONDecodeError
from requests.exceptions import ConnectionError
from statistics import median


class Payload(object):
  def __init__(self, j):
    self.code = None
    self.workTime = None
    self.message = None
    self.__dict__ = json.loads(j)


if len(sys.argv) not in (3, 4):
  print('USAGE: cold_boot_measure.py [vx|sb|ke] 100')
  exit()
target = sys.argv[1]
tests = int(sys.argv[2])

print('Running %s %d' % (target, tests))

java = 'java' if len(sys.argv) != 4 else sys.argv[3]
kumuluzee_jar = "../../kumuluzee/target/kumuluzee-1.0.0-SNAPSHOT.jar"
vertx_jar = '../../vertx/target/vertx-1.0.0-SNAPSHOT-fat.jar'
springboot_jar = '../../springboot/target/springboot-0.0.1-SNAPSHOT.jar'
quarkus_jar = '../../quarkus/target/quarkus-1.0.0-SNAPSHOT-runner.jar'
shorthand_translation = {'vx': vertx_jar,
                         'sb': springboot_jar,
                         'ke': kumuluzee_jar,
                         'qk': quarkus_jar}
run_cmd = [java, '-jar', shorthand_translation[target]]
count = 0
data = []
accumulated_fail = 0
for i in range(0, tests):
  print('\nRunning %d:%d' % (i, tests))
  start = int(time.time() * 1000)
  pid = subprocess.Popen(run_cmd)
  fail_count = 0
  request_time = 0
  decrypt_time = 0
  request_count = 0
  request_data = []
  ttfr = 0
  url_template = "http://localhost:8080/work?input={}"
  while request_count < 10:
    try:
      letters = ''.join(random.choice(string.ascii_letters) for v in range(10))
      result = requests.get(url_template.format(letters))
      payload = Payload(result.content.decode('utf-8'))
      if payload.code == 'OK':
        request_time = int(result.elapsed.total_seconds() * 1000)
        decrypt_time = payload.workTime
        request_data.append((request_time, decrypt_time))
        if request_count == 0:
          ttfr = int(time.time() * 1000) - start
        request_count += 1
      elif fail_count > 500:
        print("Too many failed decryptions, exiting.")
        exit()
      elif fail_count % 100 == 0:
        print(payload.message)
        fail_count += 1
      else:
        fail_count += 1
    except ConnectionError as err:
      time.sleep(.002)
    except JSONDecodeError as err:
      time.sleep(.002)
    except:
      print("Breaking error occurred, stopping execution.")
      pid.kill()
      exit()
    count += 1
  pid.kill()
  print('Time to first response: %d ms' % ttfr)
  print('Number of failed decryptions: %d' % fail_count)
  accumulated_fail += fail_count
  data.append((ttfr, request_data))

times = [x[0] for x in data]
request_times = [x[1][0][0] for x in data]
decrypt_times = [x[1][0][1] for x in data]
print('\nNumber of starts: %d' % len(times))
print('Number of accumulated fails: %d' % accumulated_fail)
print('\nBoot times')
print('Average response time: %d ms' % (sum(times) / len(times)))
print('Median response time: %d ms' % (median(times)))
print('Longest response time: %d ms' % (max(times)))
print('Lowest response time: %d ms' % (min(times)))
print('\nFirst request times')
print('Average response time: %d ms with %d decrypt time' % (
  sum(request_times) / len(request_times), (sum(decrypt_times) / len(decrypt_times))))
print('Median response time: %d ms with %d decrypt time' % (median(request_times), median(decrypt_times)))
print('Longest response time: %d ms with %d decrypt time' % (max(request_times), max(decrypt_times)))
print('Lowest response time: %d ms with %d decrypt time' % (min(request_times), min(decrypt_times)))

uname = platform.uname()
filename = target + '_' + str(tests) + '_' + 'times' + '_' + uname.system + '_' + uname.machine + '.txt'
with open(filename, 'w') as output:
  for start in data:
    output.write('%s,' % start[0])
    output.write(','.join([str(x[0]) for x in start[1]]))
    output.write(','.join([str(x[1]) for x in start[1]]) + '\n')
  output.write('\nNumber of starts: %d' % len(times))
  output.write('\nNumber of accumulated fails: %d' % accumulated_fail)
  output.write('\n\nBoot times')
  output.write('\nAverage response time: %d ms' % (sum(times) / len(times)))
  output.write('\nMedian response time: %d ms' % (median(times)))
  output.write('\nLongest response time: %d ms' % (max(times)))
  output.write('\nLowest response time: %d ms' % (min(times)))
  output.write('\n\nFirst request times')
  output.write('\nAverage response time: %d ms with %d decrypt time' % (
    sum(request_times) / len(request_times), (sum(decrypt_times) / len(decrypt_times))))
  output.write('\nMedian response time: %d ms with %d decrypt time' % (median(request_times), median(decrypt_times)))
  output.write('\nLongest response time: %d ms with %d decrypt time' % (max(request_times), max(decrypt_times)))
  output.write('\nLowest response time: %d ms with %d decrypt time' % (min(request_times), min(decrypt_times)))
  output.write('\n\n' + str(uname))

import json
import platform
import psutil
import requests
import subprocess
import sys
import time
from statistics import median


class Payload(object):
  def __init__(self, j):
    self.code = None
    self.workTime = None
    self.message = None
    self.__dict__ = json.loads(j)


if len(sys.argv) not in (2, 3):
  print('USAGE: serial_requests.py [vx|sb|ke]')
  exit()
target = sys.argv[1]

print('Running %s' % target)

java = 'java' if len(sys.argv) != 3 else sys.argv[2]
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
accumulated_fail = 0
request_data = []
request_count = 0
fail_count = 0

pid = subprocess.Popen(run_cmd)
pinfo = psutil.Process(pid.pid)
time.sleep(10 if target == 'vertx' else 40)  # Wait for the server to start properly.
print("Right! Enough sleep! Starting test run.")
init = int(time.time() * 1000)
with open('../../data/encrypted_messages.txt', 'r') as crypto_file:
  for line in crypto_file:
    try:
      full_roundtrip_start = int(time.time() * 1000)
      base_url = 'http://localhost:8080'
      result = requests.get(base_url + line[:-1])
      memory_info = pinfo.memory_info()[0]
      measured_request_time = int(time.time() * 1000) - full_roundtrip_start
      payload = Payload(result.content.decode('utf-8'))
      code = payload.code if hasattr(payload, 'code') else 'ERROR'
      if code == 'OK':
        reported_request_time = int(result.elapsed.total_seconds() * 1000)
        decrypt_time = payload.workTime if hasattr(payload, 'workTime') else -1
        request_data.append((measured_request_time,
                             reported_request_time,
                             decrypt_time,
                             memory_info))
        request_count += 1
      elif fail_count > 500:
        print("Too many failed decryptings, exiting.")
        break
      elif fail_count % 100 == 0:
        print(payload.message)
        fail_count += 1
      else:
        fail_count += 1
    except ConnectionError as err:
      time.sleep(.002)
    except:
      print("Breaking error occurred, stopping execution.")
      pid.kill()
      exit()
total_runtime = time.time() * 1000 - init

pid.kill()

response_times = [x[0] for x in request_data]
reported_request_times = [x[1] for x in request_data]
decrypt_times = [x[2] for x in request_data]
memory_usage = [x[3] for x in request_data]

uname = platform.uname()
filename = target + '_serial_' + uname.system + '_' + uname.machine + '.txt'
print("Saving to %s" % filename)
with open(filename, 'w') as output:
  for i in range(0, len(request_data[0])):
    output.write(','.join([str(x[i]) for x in request_data]) + '\n')
  output.write("\nTotal runtime: %d" % total_runtime)
  output.write('\nNumber of accumulated fails: %d' % fail_count)
  output.write('\n\nRequest times')
  output.write('\nAverage response time: %d ms with %d decrypt time' % (
    sum(response_times) / len(response_times), (sum(decrypt_times) / len(decrypt_times))))
  output.write('\nMedian response time: %d ms with %d decrypt time' % (median(response_times), median(decrypt_times)))
  output.write('\nLongest response time: %d ms with %d decrypt time' % (max(response_times), max(decrypt_times)))
  output.write('\nLowest response time: %d ms with %d decrypt time' % (min(response_times), min(decrypt_times)))
  output.write('\n\nMemory usage')
  output.write('\nMax memory used: %d' % max(memory_usage))
  output.write('\nMin memory used: %d' % min(memory_usage))
  output.write('\nAverage memory used: %d' % (sum(memory_usage) / len(memory_usage)))
  output.write('\nMedian memory used: %d' % median(memory_usage))
  output.write('\n\n' + str(uname))

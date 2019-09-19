import aiohttp
import asyncio
import json
import platform
import psutil
import subprocess
import sys
import time
from aiohttp import ClientSession
from statistics import median


class Payload(object):
  def __init__(self, message):
    self.status = None
    self.code = None
    self.workTime = None
    self.message = None
    try:
      self.__dict__ = json.loads(message)
    except:
      print(message)


async def send_request(url, session):
  start_request = int(time.time() * 1000)
  try:
    async with session.get(url) as response:
      data = await
      response.read()
      # cpu_percent = psutil.cpu_percent(interval=.1, percpu=False)
      memory_info = pinfo.memory_info()[0]
      payload = Payload(data.decode('utf-8'))
      return (int(time.time() * 1000) - start_request), \
             payload.code if hasattr(payload, 'code') else payload.status, \
             payload.workTime if hasattr(payload, 'workTime') else -1, \
             memory_info
  except aiohttp.ServerDisconnectedError as exc:
    return (int(time.time() * 1000) - start_request), \
           exc.code if hasattr(exc, 'code') else "ServerDisconnected", \
           -1, \
           pinfo.memory_info()[0]


async def run_load_test():
  url = "http://localhost:8080{}"
  tasks = []
  with open('../../data/encrypted_messages.txt', 'r') as input:
    async with ClientSession() as session:
      for line in input:
        task = asyncio.ensure_future(send_request(url.format(line[:-1]), session))
        tasks.append(task)
      responses.extend(await
      asyncio.gather(*tasks))
      print(len(responses))


if len(sys.argv) not in (2, 3):
  print('USAGE: crypto_load_test.py [vx|sb|ke]')
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

pid = subprocess.Popen(run_cmd)
pinfo = psutil.Process(pid.pid)
time.sleep(10 if target == 'vx' else 40)
responses = []
print("Right! Enough sleep! Starting test run.")
loop = asyncio.get_event_loop()
future = asyncio.ensure_future(run_load_test())
start = time.time() * 1000
loop.run_until_complete(future)
total_runtime = time.time() * 1000 - start
print("Total runtime %d" % total_runtime)

pid.kill()

response_times = [x[0] for x in responses]
failed_requests = sum(item[1] != 'OK' for item in responses)
decrypt_times = [x[2] for x in responses]
memory_usage = [x[3] for x in responses]

uname = platform.uname()
filename = target + '_load_test_' + uname.system + '_' + uname.machine + '.txt'
with open(filename, 'w') as output:
  for i in range(len(responses[0])):
    output.write(','.join([str(x[i]) for x in responses]) + '\n')
  output.write("\nTotal runtime: %d" % total_runtime)
  output.write('\nNumber of accumulated fails: %d' % failed_requests)
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

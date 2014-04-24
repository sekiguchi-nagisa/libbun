#!/usr/bin/env python
# -*- coding: utf-8 -*-

import sys
import os
import time
import commands
import codecs
import json
import platform
import tempfile

from bottle import Bottle, run, request, static_file

def createResponseJson(source, result, error):
    return json.dumps({'source': source, 'result': result, 'error': error})

def createSourceFile(name, contents):
    f = codecs.open(name, 'w', 'utf-8')
    f.write(contents)
    f.close()

def compileCommand(name, target, parser):
    return commands.getoutput('java -jar {0}/../libbun-0.1.jar -t {1} -p {2} {3}'.format(rootPath, target, parser, name))

def readCompiledFile(name):
    if os.path.exists(name):
        a = open(name, 'r')
        return a.read()
    return ''

#Server settings
app = Bottle()
rootPath = os.path.abspath(os.path.dirname(__file__))

#Server routings
@app.get('/')
def indexfile():
    return static_file('index.html', root=rootPath)

@app.post('/compile')
def compile():
    file = tempfile.NamedTemporaryFile(mode='w', suffix='.zen', prefix='tmp', dir='/tmp')
    name = file.name
    file.close() #tempfile cannot use utf-8 in python 2.7, so need to reopen

    if not hasattr(request, 'json'):
        return 'error'
    req = request.json

    createSourceFile(name, req["source"])
    message = compileCommand(name, req["target"], req["parser"])

    #filecontent = readCompiledFile(name)
    return createResponseJson(message, '', message)

@app.route('/samples/<filename>')
def server_static(filename):
    return static_file(filename, root=rootPath + '/../sample')

@app.route('/<filepath:path>')
def server_static(filepath):
    return static_file(filepath, root=rootPath)

run(app, host='0.0.0.0', port=3000)

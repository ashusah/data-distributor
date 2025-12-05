from flask import Flask, jsonify
import random

app = Flask(__name__)

@app.route('/create-signal/write-signal', methods=['POST'])
def write_signal():
    return jsonify({'ceh_event_id': random.randint(100000, 999999)}), 200

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)

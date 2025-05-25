from flask import Flask, request, jsonify
from flask_limiter import Limiter
from flask_limiter.util import get_remote_address

app = Flask(__name__)

limiter = Limiter(
    key_func=get_remote_address,
    default_limits=["50 per minute"]
)
limiter.init_app(app)

@app.route('/test', methods=['GET', 'POST'])
@limiter.limit("50 per minute")  # Optional if you want per-route limit
def test_endpoint():
    if request.method == 'POST':
        data = request.get_json() or {}
        return jsonify({
            "message": "POST request received",
            "your_data": data
        })
    else:
        return jsonify({
            "message": "GET request received"
        })

if __name__ == '__main__':
    app.run(debug=True, port=5000)

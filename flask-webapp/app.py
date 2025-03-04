from flask import Flask, render_template, request, redirect, url_for, flash
import json
import static_data
import requests
import os
import time

app = Flask(__name__)
app.secret_key = os.urandom(24)

GATEWAY_URL = 'http://gateway:8090'

@app.route('/')
def home():

    context = {
        'employees': [
            (
                index,
                employee['firstname'],
                employee['lastname'],
                employee.get('specialty', None),
                employee.get('cost', None),
            )
            for index, employee in enumerate(static_data.employees)
        ]
    }


    return render_template('index.html', **context)


def send_request_to_gateway(json_data, url=f'{GATEWAY_URL}/api/visit/booking', request_type='POST'):
    headers = {
        'Accept': 'application/json',
        'Content-Type': 'application/json'
    }

    try:
        # Send the HTTP request (POST by default)
        response = requests.post(url, headers=headers, data=json_data)

        # Check if the response was successful (status code 2xx)
        if response.status_code == 200:
            print("Request was successful!")
            print("Response:", response.json())  # If response is JSON
            return response.json().get("id")
        else:
            print(f"Error: {response.status_code}, {response.text}")
    except requests.exceptions.RequestException as e:
        # Handle any errors during the request
        print(f"An error occurred: {e}")

def check_booking_status(booking_id):
    url=f'{GATEWAY_URL}/api/visit/result/{booking_id}'
    try:
        response = requests.get(url)

        if response.status_code == 200:
            success = response.json()
            print("Booking cost:", success)
            return success
        else:
            print(f"Error: {response.status_code}, {response.text}")
            return None
    except requests.exceptions.RequestException as e:
        print(f"An error occurred while checking status: {e}")
        return None

@app.route('/visit/booking', methods=['POST'])
def submit():
    # Extract data from the form
    customer_firstname = request.form.get('customer_firstname')
    customer_lastname = request.form.get('customer_lastname')

    card_number = request.form.get('card_number')
    card_firstname = request.form.get('card_firstname')
    card_lastname = request.form.get('card_lastname')

    visit_start = request.form.get('visit_start')
    visit_stop = request.form.get('visit_stop')

    employee_index = request.form['employee_index']
    employee_data = static_data.employees[int(employee_index)]

    print(f"Customer Name: {customer_firstname}")
    print(f"Customer Surname: {customer_lastname}")

    print(f"Card Number: {card_number}")
    print(f"Card Name: {card_firstname}")
    print(f"Card Surname: {card_lastname}")


    print(f"Visit Start: {visit_start}")
    print(f"Visit Stop: {visit_stop}")

    data = {
        "customer": {
            "firstName": customer_firstname,
            "lastName": customer_lastname,
        },
        "card": {
            "number": card_number,
            "firstName": card_firstname,
            "lastName": card_lastname,
        },
        "employee": {
            "firstName": employee_data['firstname'],
            "lastName": employee_data['lastname'],
        },
        "visit": {
            "dateStart": visit_start,
            "dateStop": visit_stop
        }
    }

    # Convert to JSON string (optional, for sending to a REST API)
    json_data = json.dumps(data)

    print(json_data)

    id = send_request_to_gateway(json_data)
    
    time.sleep(1)

    if id: # Zakładam, że zwracane jest pole "id"
        cost = check_booking_status(id)

        if cost:
            flash("Wizyta poprawnie zarezerwowana. Id wizyty: " + id + "; koszt wizyty: " + cost + " zł", "success")    
        else:
            flash("Wystąpił błąd podczas rezerwacji wizyty. Sprawdź poprawność danych lub zmień termin wizyty.", "error")
    else:
        flash("Błąd podczas tworzenia rezerwacji.", "error")


    return redirect(url_for('home'))

# Run the app
if __name__ == '__main__':
    app.run(debug=True)
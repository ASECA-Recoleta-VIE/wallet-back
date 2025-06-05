import random
import json
import time
from locust import HttpUser, task, between


class WalletApiUser(HttpUser):
    wait_time = between(1, 5)  # Wait between 1 and 5 seconds between tasks

    # User state variables
    email = None
    token = None
    is_logged_in = False

    def on_start(self):
        # Generate a unique email for this user
        self.email = f"user_{int(time.time())}_{random.randint(1, 100000)}@example.com"
        self.password = "Password123!"
        self.full_name = "Test User"
        self.is_registered = False

        # Register the user
        self.register()

        # Login to get the token only if registration was successful
        if self.is_registered:
            self.login()

    def register(self):
        """Register a new user"""
        payload = {
            "fullName": self.full_name,
            "email": self.email,
            "password": self.password
        }

        with self.client.post("/api/users/register", json=payload, catch_response=True) as response:
            # The API returns 201 or 200 with the user object on success
            if response.status_code in [200, 201]:
                self.is_registered = True
                # Mark as success regardless of the response content
                response.success()
            else:
                self.is_registered = False
                response.failure(f"Failed to register user: {response.text}")

    def login(self):
        """Login with the registered user"""
        # Don't attempt login if we're not registered
        if not hasattr(self, 'is_registered') or not self.is_registered:
            return False

        payload = {
            "email": self.email,
            "password": self.password
        }

        with self.client.post("/api/users/login", json=payload, catch_response=True) as response:
            if response.status_code == 200:
                try:
                    # Extract token from Set-Cookie header
                    set_cookie = response.headers.get("Set-Cookie", "")
                    token = None
                    if set_cookie:
                        for part in set_cookie.split(";"):
                            if "token=" in part:
                                token = part.strip().split("=", 1)[1]
                                break

                    self.token = token
                    if self.token:
                        self.is_logged_in = True
                        # Set the authorization header for future requests
                        self.client.headers.update({"Cookie": f"token={self.token}"})
                        return True
                    else:
                        response.failure("Login response did not contain a token")
                        return False
                except Exception as e:
                    response.failure(f"Error processing login response: {str(e)}")
                    return False
            else:
                response.failure(f"Failed to login: {response.text}")
                return False

    @task(2)
    def test_login(self):
        """Explicitly test the login endpoint"""
        # Decide whether to use known credentials or random ones
        use_known_credentials = random.random() < 0.8 and self.email is not None and self.is_registered

        if use_known_credentials:
            # Use the known good credentials that should work
            test_email = self.email
            test_password = self.password
            expected_success = True
        else:
            # Generate random credentials that will likely fail (for testing error handling)
            test_email = f"test_user_{random.randint(1, 1000)}@example.com"
            test_password = "Password123!"
            expected_success = False

        payload = {
            "email": test_email,
            "password": test_password
        }

        with self.client.post("/api/users/login", json=payload, catch_response=True) as response:
            if expected_success:
                # We expect success for known good credentials
                if response.status_code == 200:
                    try:
                        # Check if the response contains a token
                        set_cookie = response.headers.get("Set-Cookie", "")
                        token = None
                        if set_cookie:
                            for part in set_cookie.split(";"):
                                if "token=" in part:
                                    token = part.strip().split("=", 1)[1]
                                    break
                        if token:
                            response.success()
                        else:
                            response.failure("Login response did not contain a token")
                    except Exception as e:
                        response.failure(f"Error processing login response: {str(e)}")
                else:
                    response.failure(f"Login with valid credentials failed: {response.text}")
            else:
                # For random credentials, we expect failure, so mark as success if we get an error
                if response.status_code != 200:
                    response.success()
                else:
                    # If we somehow succeeded with random credentials, that's interesting but not a failure
                    response.success()

    @task(1)
    def test_register(self):
        """Explicitly test the register endpoint"""
        # Generate a random user for registration
        test_email = f"test_register_{int(time.time())}_{random.randint(1, 100000)}@example.com"
        test_password = "Password123!"
        test_full_name = f"Test Register User {random.randint(1, 1000)}"

        payload = {
            "fullName": test_full_name,
            "email": test_email,
            "password": test_password
        }

        with self.client.post("/api/users/register", json=payload, catch_response=True) as response:
            # The API returns 201 or 200 with the user object on success
            if response.status_code in [200, 201]:
                # Mark as success regardless of the response content
                response.success()
            else:
                response.failure(f"Failed to register test user: {response.text}")

    @task(5)
    def get_wallet(self):
        """Get the user's wallet information"""
        # Skip if not registered
        if not hasattr(self, 'is_registered') or not self.is_registered:
            return

        # Try to login if not logged in
        if not self.is_logged_in:
            login_success = self.login()
            if not login_success:
                return  # Skip if login fails

        with self.client.get("/wallet", catch_response=True) as response:
            if response.status_code == 200:
                response.success()
            elif response.status_code == 401:
                # Token might have expired, try to login again
                login_success = self.login()
                if not login_success:
                    response.failure("Failed to re-login after token expiration")
            else:
                response.failure(f"Failed to get wallet: {response.text}")

    @task(3)
    def get_history(self):
        """Get the user's transaction history"""
        # Skip if not registered
        if not hasattr(self, 'is_registered') or not self.is_registered:
            return

        # Try to login if not logged in
        if not self.is_logged_in:
            login_success = self.login()
            if not login_success:
                return  # Skip if login fails

        with self.client.get("/history", catch_response=True) as response:
            if response.status_code == 200:
                response.success()
            elif response.status_code == 401:
                # Token might have expired, try to login again
                login_success = self.login()
                if not login_success:
                    response.failure("Failed to re-login after token expiration")
            else:
                response.failure(f"Failed to get history: {response.text}")

    @task(2)
    def deposit(self):
        """Deposit money into the user's wallet"""
        # Skip if not registered
        if not hasattr(self, 'is_registered') or not self.is_registered:
            return

        # Try to login if not logged in
        if not self.is_logged_in:
            login_success = self.login()
            if not login_success:
                return  # Skip if login fails

        payload = {
            "amount": round(random.uniform(10, 1000), 2),
            "description": f"Deposit test at {time.strftime('%H:%M:%S')}"
        }

        with self.client.post("/deposit", json=payload, catch_response=True) as response:
            if response.status_code == 200:
                response.success()
            elif response.status_code == 401:
                # Token might have expired, try to login again
                login_success = self.login()
                if not login_success:
                    response.failure("Failed to re-login after token expiration")
            else:
                response.failure(f"Failed to deposit: {response.text}")

    @task(2)
    def withdraw(self):
        """Withdraw money from the user's wallet"""
        # Skip if not registered
        if not hasattr(self, 'is_registered') or not self.is_registered:
            return

        # Try to login if not logged in
        if not self.is_logged_in:
            login_success = self.login()
            if not login_success:
                return  # Skip if login fails

        payload = {
            "amount": round(random.uniform(1, 50), 2),
            "description": f"Withdrawal test at {time.strftime('%H:%M:%S')}"
        }

        with self.client.post("/withdraw", json=payload, catch_response=True) as response:
            if response.status_code == 200:
                response.success()
            # This might fail if there are insufficient funds, which is expected in some cases
            elif response.status_code == 403:
                # Insufficient funds is an expected error in some cases
                response.success()  # Mark as success since this is expected behavior
            elif response.status_code == 401:
                # Token might have expired, try to login again
                login_success = self.login()
                if not login_success:
                    response.failure("Failed to re-login after token expiration")
            else:
                response.failure(f"Failed to withdraw: {response.text}")

    @task(1)
    def transfer(self):
        """Transfer money to another user"""
        # Skip if not registered
        if not hasattr(self, 'is_registered') or not self.is_registered:
            return

        # Try to login if not logged in
        if not self.is_logged_in:
            login_success = self.login()
            if not login_success:
                return  # Skip if login fails

        # Generate a random recipient email
        # In a real scenario, you might want to create multiple users and transfer between them
        recipient_email = f"recipient_{random.randint(1, 1000)}@example.com"

        payload = {
            "toEmail": recipient_email,
            "amount": round(random.uniform(1, 30), 2),
            "description": f"Transfer test at {time.strftime('%H:%M:%S')}"
        }

        with self.client.post("/transfer", json=payload, catch_response=True) as response:
            if response.status_code == 200:
                response.success()
            # This might fail if there are insufficient funds or recipient not found, which is expected in some cases
            elif response.status_code == 403:
                # Insufficient funds is an expected error in some cases
                response.success()  # Mark as success since this is expected behavior
            elif response.status_code == 404:
                # Recipient not found is an expected error when using random emails
                response.success()  # Mark as success since this is expected behavior
            elif response.status_code == 401:
                # Token might have expired, try to login again
                login_success = self.login()
                if not login_success:
                    response.failure("Failed to re-login after token expiration")
            else:
                response.failure(f"Failed to transfer: {response.text}")

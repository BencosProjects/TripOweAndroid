import os
import re
from datetime import datetime

class Participant:
    def __init__(self, id, name):
        self.id = id
        self.name = name

class Expense:
    def __init__(self, id, description, amount, payer_id):
        self.id = id
        self.description = description
        self.amount = amount
        self.payer_id = payer_id

class TripDataManager:
    def __init__(self, file_path="AppRepository.kt"):
        self.file_path = file_path
        self.participants = []
        self.expenses = []
        self.load_data()

    def load_data(self):
        """Load participants and expenses from AppRepository.kt"""
        try:
            with open(self.file_path, 'r', encoding='utf-8') as file:
                content = file.read()

            # Extract _participants
            participants_match = re.search(r'private val _participants = MutableStateFlow\(listOf\((.*?)\)\)', content, re.DOTALL)
            if participants_match:
                participants_text = participants_match.group(1).strip()
                participant_lines = [line.strip() for line in participants_text.split('\n') if line.strip() and not line.strip().startswith('//')]
                for line in participant_lines:
                    match = re.match(r'Participant\((\d+), "(.*?)"\)', line.rstrip(','))
                    if match:
                        id, name = int(match.group(1)), match.group(2)
                        self.participants.append(Participant(id, name))

            # Extract _expenses
            expenses_match = re.search(r'private val _expenses = MutableStateFlow\(listOf\((.*?)\)\)', content, re.DOTALL)
            if expenses_match:
                expenses_text = expenses_match.group(1).strip()
                expense_lines = [line.strip() for line in expenses_text.split('\n') if line.strip() and not line.strip().startswith('//')]
                for line in expense_lines:
                    match = re.match(r'Expense\((\d+), "(.*?)", ([\d.]+), (\d+)\)', line.rstrip(','))
                    if match:
                        id, desc, amount, payer_id = int(match.group(1)), match.group(2), float(match.group(3)), int(match.group(4))
                        self.expenses.append(Expense(id, desc, amount, payer_id))

        except FileNotFoundError:
            print(f"Error: File {self.file_path} not found")
            self.participants = [
                Participant(1, "אליס"),
                Participant(2, "בוב"),
                Participant(3, "צ'ארלי")
            ]
            self.expenses = [
                Expense(1, "בנזין", 1000.0, 1),
                Expense(2, "אוכל", 800.0, 2),
                Expense(3, "לינה", 600.0, 3)
            ]

    def save_data(self):
        """Update AppRepository.kt with current participants and expenses"""
        try:
            with open(self.file_path, 'r', encoding='utf-8') as file:
                content = file.read()

            # Backup original file
            backup_path = self.file_path + f".backup_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
            with open(backup_path, 'w', encoding='utf-8') as backup_file:
                backup_file.write(content)

            # Generate new participants code
            participants_code = "private val _participants = MutableStateFlow(listOf(\n"
            for p in self.participants:
                participants_code += f'    Participant({p.id}, "{p.name}"),\n'
            participants_code += "))"
            content = re.sub(r'private val _participants = MutableStateFlow\(listOf\((.*?)\)\)', participants_code, content, flags=re.DOTALL)

            # Generate new expenses code
            expenses_code = "private val _expenses = MutableStateFlow(listOf(\n"
            for e in self.expenses:
                expenses_code += f'    Expense({e.id}, "{e.description}", {e.amount}, {e.payer_id}),\n'
            expenses_code += "))"
            content = re.sub(r'private val _expenses = MutableStateFlow\(listOf\((.*?)\)\)', expenses_code, content, flags=re.DOTALL)

            # Save updated content
            with open(self.file_path, 'w', encoding='utf-8') as file:
                file.write(content)
            print(f"File {self.file_path} updated successfully. Backup saved to {backup_path}")

        except FileNotFoundError:
            print(f"Error: File {self.file_path} not found")
        except Exception as e:
            print(f"Error saving file: {str(e)}")

    def add_participant(self, name):
        if not name.strip():
            print("Error: Name cannot be empty")
            return
        new_id = max([p.id for p in self.participants], default=0) + 1
        self.participants.append(Participant(new_id, name))
        print(f"Added participant: {name} (ID: {new_id})")
        self.save_data()

    def remove_participant(self, id):
        if any(e.payer_id == id for e in self.expenses):
            print(f"Cannot remove participant with ID {id} because they have expenses")
            return
        if not any(p.id == id for p in self.participants):
            print(f"Error: No participant with ID {id}")
            return
        self.participants = [p for p in self.participants if p.id != id]
        print(f"Removed participant with ID {id}")
        self.save_data()

    def add_expense(self, description, amount, payer_id):
        if not description.strip():
            print("Error: Description cannot be empty")
            return
        if amount <= 0:
            print("Error: Amount must be positive")
            return
        if not any(p.id == payer_id for p in self.participants):
            print(f"Error: No participant with ID {payer_id}")
            return
        new_id = max([e.id for e in self.expenses], default=0) + 1
        self.expenses.append(Expense(new_id, description, amount, payer_id))
        print(f"Added expense: {description}, {amount}$ (ID: {new_id}, Payer: {payer_id})")
        self.save_data()

    def remove_expense(self, id):
        if not any(e.id == id for e in self.expenses):
            print(f"Error: No expense with ID {id}")
            return
        self.expenses = [e for e in self.expenses if e.id != id]
        print(f"Removed expense with ID {id}")
        self.save_data()

    def list_participants(self):
        print("\nList of participants:")
        if not self.participants:
            print("No participants")
        for p in self.participants:
            print(f"ID: {p.id}, Name: {p.name}")

    def list_expenses(self):
        print("\nList of expenses:")
        if not self.expenses:
            print("No expenses")
        for e in self.expenses:
            payer_name = next((p.name for p in self.participants if p.id == e.payer_id), "Unknown")
            print(f"ID: {e.id}, Description: {e.description}, Amount: {e.amount}$, Payer: {payer_name} (ID: {e.payer_id})")

def main():
    manager = TripDataManager()
    while True:
        print("\n=== Trip Data Manager (Updates AppRepository.kt) ===")
        print("1. Add participant")
        print("2. Remove participant")
        print("3. Add expense")
        print("4. Remove expense")
        print("5. List participants")
        print("6. List expenses")
        print("7. Exit")
        choice = input("Choose an action (1-7): ")

        if choice == "1":
            name = input("Enter participant name: ")
            manager.add_participant(name)

        elif choice == "2":
            manager.list_participants()
            try:
                id = int(input("Enter participant ID to remove: "))
                manager.remove_participant(id)
            except ValueError:
                print("Error: Enter a valid ID")

        elif choice == "3":
            manager.list_participants()
            description = input("Enter expense description: ")
            try:
                amount = float(input("Enter amount: "))
                payer_id = int(input("Enter payer ID: "))
                manager.add_expense(description, amount, payer_id)
            except ValueError:
                print("Error: Enter valid amount or ID")

        elif choice == "4":
            manager.list_expenses()
            try:
                id = int(input("Enter expense ID to remove: "))
                manager.remove_expense(id)
            except ValueError:
                print("Error: Enter a valid ID")

        elif choice == "5":
            manager.list_participants()

        elif choice == "6":
            manager.list_expenses()

        elif choice == "7":
            print("Exiting...")
            break

        else:
            print("Invalid choice, try again")

if __name__ == "__main__":
    main()
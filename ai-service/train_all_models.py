from train_traffic_model import train_traffic_model
from train_staff_model import train_staff_model
from train_delay_model import train_delay_model


def main():
    print("Pornire antrenare modele pentru sistemul de decizie...")
    print()

    train_traffic_model()
    print()

    train_staff_model()
    print()

    train_delay_model()
    print()

    print("Toate modelele au fost antrenate cu succes.")


if __name__ == "__main__":
    main()
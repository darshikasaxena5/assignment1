# 📈 Stock Explorer

A modern stock market tracking app built using **Kotlin + Jetpack Compose**. The app provides real-time stock data, gainers/losers insights, price charts, and personalized watchlists — all in a clean and intuitive UI.

---

## 🚀 Features

### 🔍 Explore Tab
- **Top Gainers** – Shows stocks with the highest price increase for the day.
- **Top Losers** – Shows stocks with the biggest declines.
- **Most Active** – High-volume stocks based on market data.

### 📊 Stock Detail View
- Real-time price
- 30-day simulated price chart
- Key statistics like symbol & volume
- Watchlist toggle (bookmark-style)

### 🔖 Watchlist Management
- Create and delete custom watchlists
- Add stocks to a specific watchlist
- Validate ticker symbols before adding
- Swipe-to-delete stock entries



---

## 🛠️ Tech Stack

- **Kotlin**
- **Jetpack Compose**
- **MVVM Architecture**
- **Coroutines + Flow**
- **Alpha Vantage API** (or simulated stock data)

---

## 📦 How to Run

1. Clone this repo:
   ```bash
   git clone https://github.com/your-username/your-repo-name.git

**2. Open in Android Studio**
- Open Android Studio
- Click on **"Open an Existing Project"**
- Select the project folder you just cloned

**3. Add your Alpha Vantage API Key**
- In the root folder, open `local.properties`
- Add the following line:

ALPHA_VANTAGE_API_KEY=your_api_key_here

**4. Run the app**
- Select your emulator or connected device
- Click the green **"Run"** ▶️ button or press `Shift + F10`

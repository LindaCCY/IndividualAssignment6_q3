# Sound Meter App
An Android app that measures sound levels in real-time and displays them as decibel (dB) values.

## Features
- Realtime sound level measurement using microphone
- Converts audio amplitude to decibel  values
- Visual sound meter with progress bar/colored indicator
- Alert notification when noise exceeds threshold

## How It Works
The app uses AudioRecord to capture audio data from the microphone and calculates decibel levels using the formula:
dB = 20 * log10(amplitude / reference)
Visual indicators change color based on sound intensity, and alerts trigger when levels exceed safe thresholds.

## Setup
Clone the repository
Open in Android Studio
Build and run on device
Grant microphone permission when prompted

import { useEffect, useRef, useState } from 'react'
import type { StompSubscription } from '@stomp/stompjs'
import reactLogo from './assets/react.svg'
import viteLogo from './assets/vite.svg'
import heroImg from './assets/hero.png'
import './App.css'

import { stompClient } from './services/mlpStompClient'
import type { TrainingEvent } from './types/TrainingEvent'

const TRAINING_SESSION_ID = '1'

function App() {
  const [mlpStatus, setMlpStatus] = useState<TrainingEvent | null>(null)
  const [training, setTraining] = useState<boolean>(false)
  const [mlpClientConnected, setMlpClientConnected] = useState<boolean>(false)
  const trainingSubscriptionRef = useRef<StompSubscription | null>(null)

  useEffect(() => {
    return () => {
      trainingSubscriptionRef.current?.unsubscribe()
      void stompClient.disconnect()
    }
  }, [])

  function handleTrainingEvent(event: TrainingEvent): void {
    setMlpStatus(event)
    setTraining(true)
  }

  async function handleStartMushroomsTraining(): Promise<void> {
    try {
      if (!stompClient.connected) {
        await stompClient.connect()
        setMlpClientConnected(true)
      }

      if (!trainingSubscriptionRef.current) {
        trainingSubscriptionRef.current = stompClient.subscribeToTrainingStatus(
          TRAINING_SESSION_ID,
          handleTrainingEvent,
        )
      }

      setTraining(true)
      stompClient.startMushroomsTraining(TRAINING_SESSION_ID)
    } catch (error) {
      setTraining(false)
      setMlpClientConnected(false)
      console.error(error)
    }
  }

  return (
    <>
      <section id="center">
        <div className="hero">
          <img src={heroImg} className="base" width="170" height="179" alt="" />
          <img src={reactLogo} className="framework" alt="React logo" />
          <img src={viteLogo} className="vite" alt="Vite logo" />
        </div>
        <div>
          <h1>Get started</h1>
          <p>
            Edit <code>src/App.tsx</code> and save to test <code>HMR</code>
          </p>
        </div>
        <button
          type="button"
          className="counter"
          onClick={async () => {
            if (!stompClient.connected) {
              try {
                await stompClient.connect()
                setMlpClientConnected(true)
              } catch (error) {
                setMlpClientConnected(false)
                console.error(error)
              }
            }
          }}
        >
          {mlpClientConnected ? 'Conectado' : 'Conectar'}
        </button>
      </section>

      <div className="ticks"></div>

      <section id="next-steps">
        <div id="docs">
          <svg className="icon" role="presentation" aria-hidden="true">
            <use href="/icons.svg#documentation-icon"></use>
          </svg>
          <h2>Mushrooms test</h2>
          <ul>
            <li>
              <button
                type="button"
                className="counter"
                onClick={handleStartMushroomsTraining}
                disabled={training}
              >
                {training ? 'Treinando...' : 'Começar'}
              </button>
              <p>
                {mlpStatus
                  ? `Reacting to training event: ${mlpStatus.type} | epoch ${mlpStatus.epoch} | sample ${mlpStatus.sampleIndex} | error ${mlpStatus.networkError}`
                  : 'Reacting to training event: waiting for training to start'}
              </p>
            </li>
          </ul>
        </div>
        <div id="social">
          <svg className="icon" role="presentation" aria-hidden="true">
            <use href="/icons.svg#social-icon"></use>
          </svg>
          <h2>Connect with us</h2>
          <p>Join the Vite community</p>
          <ul>
            <li>
              <a href="https://github.com/vitejs/vite" target="_blank">
                <svg
                  className="button-icon"
                  role="presentation"
                  aria-hidden="true"
                >
                  <use href="/icons.svg#github-icon"></use>
                </svg>
                GitHub
              </a>
            </li>
            <li>
              <a href="https://chat.vite.dev/" target="_blank">
                <svg
                  className="button-icon"
                  role="presentation"
                  aria-hidden="true"
                >
                  <use href="/icons.svg#discord-icon"></use>
                </svg>
                Discord
              </a>
            </li>
            <li>
              <a href="https://x.com/vite_js" target="_blank">
                <svg
                  className="button-icon"
                  role="presentation"
                  aria-hidden="true"
                >
                  <use href="/icons.svg#x-icon"></use>
                </svg>
                X.com
              </a>
            </li>
            <li>
              <a href="https://bsky.app/profile/vite.dev" target="_blank">
                <svg
                  className="button-icon"
                  role="presentation"
                  aria-hidden="true"
                >
                  <use href="/icons.svg#bluesky-icon"></use>
                </svg>
                Bluesky
              </a>
            </li>
          </ul>
        </div>
      </section>

      <div className="ticks"></div>
      <section id="spacer"></section>
    </>
  )
}

export default App

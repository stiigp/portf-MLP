import {
  Client,
  type Frame,
  type IMessage,
  type StompSubscription,
} from '@stomp/stompjs'
import type { StartTrainingPayload } from '../types/StartTrainingPayload'
import type { TrainingEvent } from '../types/TrainingEvent'
import { apiWebSocketUrl } from '../config/apiConfig'

type TrainingEventHandler = (event: TrainingEvent) => void
type StompErrorHandler = (frame: Frame) => void
type WebSocketErrorHandler = (event: Event) => void

interface MlpStompClientOptions {
  brokerURL?: string
  reconnectDelay?: number
  heartbeatIncoming?: number
  heartbeatOutgoing?: number
  debug?: boolean
}

const DEFAULT_BROKER_URL = apiWebSocketUrl('/ws')

export class MlpStompClient {
  private readonly client: Client
  private connectPromise: Promise<void> | null = null
  private onStompErrorHandler?: StompErrorHandler
  private onWebSocketErrorHandler?: WebSocketErrorHandler

  constructor(options: MlpStompClientOptions = {}) {
    this.client = new Client({
      brokerURL: options.brokerURL ?? DEFAULT_BROKER_URL,
      reconnectDelay: options.reconnectDelay ?? 5000,
      heartbeatIncoming: options.heartbeatIncoming ?? 4000,
      heartbeatOutgoing: options.heartbeatOutgoing ?? 4000,
      debug: options.debug
        ? (message) => {
            console.log('[STOMP]', message)
          }
        : undefined,
    })

    this.client.onStompError = (frame) => {
      console.error('Erro STOMP:', frame.headers['message'])
      console.error(frame.body)
      this.onStompErrorHandler?.(frame)
    }

    this.client.onWebSocketError = (event) => {
      console.error('Erro WebSocket:', event)
      this.onWebSocketErrorHandler?.(event)
    }
  }

  get connected(): boolean {
    return this.client.connected
  }

  connect(): Promise<void> {
    if (this.client.connected) {
      return Promise.resolve()
    }

    if (this.connectPromise) {
      return this.connectPromise
    }

    this.connectPromise = new Promise((resolve, reject) => {
      this.client.onConnect = () => {
        console.log('Conectado ao servidor STOMP')
        this.connectPromise = null
        resolve()
      }

      this.client.onWebSocketClose = (event) => {
        if (this.connectPromise) {
          this.connectPromise = null
          reject(event)
        }
      }

      this.client.activate()
    })

    return this.connectPromise
  }

  async disconnect(): Promise<void> {
    this.connectPromise = null
    await this.client.deactivate()
  }

  subscribeToTrainingStatus(
    sessionId: string,
    onEvent: TrainingEventHandler,
  ): StompSubscription {
    this.assertConnected()

    return this.client.subscribe(
      `/topic/mlp/${sessionId}/status`,
      (message: IMessage) => {
        const event = JSON.parse(message.body) as TrainingEvent
        onEvent(event)
      },
    )
  }

  startTraining(payload: StartTrainingPayload): void {
    this.assertConnected()

    this.client.publish({
      destination: '/app/mlp/start',
      body: JSON.stringify(payload),
    })
  }

  pauseTraining(sessionId: string): void {
    this.assertConnected()

    this.client.publish({
      destination: `/app/mlp/${sessionId}/pause`,
    })
  }

  startMushroomsTraining(sessionId = '1'): void {
    this.startTraining({
      sessionId,
      databaseName: 'mushrooms',
      hiddenLayersNumber: 1,
      activationFunctionName: 'logistic',
      learningRate: 0.0001,
      stopError: 0.001,
      maxEpochs: 200,
      eventOptions: {
        progressSampleInterval: 10,
        outputSampleInterval: 100,
        weightsSampleInterval: 50,
        progressMinMillis: 100,
        weightsMinMillis: 250,
      },
    })
  }

  onStompError(handler: StompErrorHandler): void {
    this.onStompErrorHandler = handler
  }

  onWebSocketError(handler: WebSocketErrorHandler): void {
    this.onWebSocketErrorHandler = handler
  }

  private assertConnected(): void {
    if (!this.client.connected) {
      throw new Error('STOMP client is not connected. Call connect() first.')
    }
  }
}

export const stompClient = new MlpStompClient({
  debug: true,
})

export default stompClient

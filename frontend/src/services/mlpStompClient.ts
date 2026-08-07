import { Client, IMessage, StompSubscription } from "@stomp/stompjs";
import type { TrainingEvent } from "../types/TrainingEvent";
import type { StartTrainingPayload } from "../types/StartTrainingPayload";

const stompClient = new Client({
  brokerURL: "ws://localhost:8080/ws",

  reconnectDelay: 5000,

  heartbeatIncoming: 4000,
  heartbeatOutgoing: 4000,

  debug: (message) => {
    console.log("[STOMP]", message);
  },
});

stompClient.onConnect = () => {
  console.log("Conectado ao servidor STOMP");
  const sessionId: string = "1";
    
  stompClient.publish(
      {
          destination: "/app/mlp/start"
      }
  )

  stompClient.subscribe(`/topic/mlp/${sessionId}`, (message: IMessage) => {
    const payload: TrainingEvent = JSON.parse(message.body);

    console.log(payload.type);
    console.log(payload.epoch);
    console.log(payload.networkError);
  });
};

stompClient.onStompError = (frame) => {
  console.error("Erro STOMP:", frame.headers["message"]);
  console.error(frame.body);
};

stompClient.onWebSocketError = (error) => {
  console.error("Erro WebSocket:", error);
};

stompClient.activate();

interface message {
  type: "enter" | "leave" | "chat" | "current_players" | "error";
  event?: "creator" | "creator_change" | "player";
  nickname?: string;
  token?: string;
  message: string;
  playerCount?: number;
  newCreatorToken?: string;
  playerId?: string;
  image?: number;
  players?: {
    playerId: string;
    nickname: string;
    image: number;
    authorization: "creator" | "player";
  }[];
}

interface player {
  playerId: string;
  image: number;
  nickname: string;
  isHost: boolean;
  isReady: boolean;
  isEmpty: boolean;
}

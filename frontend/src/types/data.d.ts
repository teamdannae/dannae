interface room {
  roomId?: number;
  id?: string;
  title: string;
  mode: "무한 초성 지옥" | "단어의 방";
  release: boolean;
  code: string;
  creator?: number;
  creatorNickname?: string;
  isPublic?: boolean;
  playerCount: number;
}

interface message {
  type:
    | "enter"
    | "leave"
    | "chat"
    | "current_players"
    | "status_update"
    | "game_start_ready"
    | "game_start"
    | "error"
    | "answer";
  event?: "creator" | "creator_change" | "player" | "rejoin_waiting";
  creatorId?: string;
  nickname?: string;
  token?: string;
  message: string;
  playerCount?: number;
  newCreatorToken?: string;
  playerId?: string;
  status?: string;
  image?: number;
  players?: {
    playerId: string;
    nickname: string;
    image: number;
    authorization: "creator" | "player";
    token: string;
  }[];
  room?: room;
  word?: string;
}

interface chat {
  type: string;
  playerId: string;
  message?: string;
}

interface player {
  playerId: string;
  image: number;
  nickname: string;
  isHost: boolean;
  isReady: boolean;
  isEmpty: boolean;
}

interface gameroom {
  roomId: number;
  title: string;
  mode: string;
  playerCount: number;
  creator: number;
  isEmpty?: boolean;
}

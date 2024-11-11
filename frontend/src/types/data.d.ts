interface room {
  roomId?: number;
  id?: string;
  title: string;
  mode: "무한 초성 지옥" | "단어의 방";
  release: boolean;
  code: string;
  creator?: number;
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
    | "answer_result"
    | "infiniteGameStart"
    | "turn_info"
    | "round_start"
    | "round_end";
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
  }[];
  room?: room;
  word?: string;
  words?: {
    word: string;
    difficulty: number;
  }[];
  initial?: string;
  // 무한 초성 지옥 정답 유무 데이터
  data?: InfiniteWord;
  round?: string;
  userWords: string[];
  playerDtos: SentencePlayer[];
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
  isTurn: boolean;
  nowScore: number;
  totalScore: number;
}

interface gameroom {
  roomId: number;
  title: string;
  mode: string;
  playerCount: number;
  creator: number;
  isEmpty?: boolean;
  creatorNickname?: string;
}

interface word {
  correct?: boolean;
  used?: boolean;
  difficulty: number;
  reason?: string;
  word: string;
}

interface SentencePlayer {
  playerId: number;
  nickname: string;
  playerCorrects: number;
  playerNowScore: number;
  playerTotalScore: number;
  playerSentence: string;
}

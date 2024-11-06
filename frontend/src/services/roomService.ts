export const fetchRooms = async () => {
  const response = await fetch("http://dannae.kr/api/v1/rooms/list", {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
    },
    credentials: "include",
  });

  console.log(response);

  if (!response.ok) {
    throw new Error("Failed to fetch rooms");
  }

  return response.json();
};

export const createRoom = async () => {
  console.log("방 생성");
};

interface InfiniteProps {
  wordList: string[];
}

const Infinite = ({ wordList }: InfiniteProps) => {
  return (
    <div>
      <h1>무한 초성 게임</h1>
      <h3>{wordList}</h3>
    </div>
  );
};

export default Infinite;

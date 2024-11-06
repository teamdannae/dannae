import requests
import pandas as pd
from time import sleep
import xml.etree.ElementTree as ET

def get_consonant(char):
    """한글 문자에서 초성을 추출합니다."""
    CONSONANTS = ['ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ']
    if '가' <= char <= '힣':
        char_code = ord(char) - ord('가')
        consonant_index = char_code // (21 * 28)
        return CONSONANTS[consonant_index]
    return char

def get_word_consonants(word):
    """단어의 초성들을 추출합니다."""
    return ''.join(get_consonant(char) for char in word)

def read_words(file_path):
    """텍스트 파일에서 단어 목록을 읽어옵니다."""
    with open(file_path, 'r', encoding='utf-8') as file:
        return [line.strip() for line in file if line.strip()]

def search_word(word, api_key):
    """표준국어대사전 API를 통해 단어를 검색합니다."""
    if len(word) == 1:
        print(f"건너뜀: {word} (한 글자)")
        return None
        
    url = 'https://stdict.korean.go.kr/api/search.do'
    params = {
        'key': api_key,
        'q': word,
        'req_type': 'xml',
        'start': 1,
        'num': 10,
        'advanced': 'y'
    }
    
    try:
        response = requests.get(url, params=params)
        
        if '<error>' in response.text:
            print(f"검색 실패: {word}")
            return None
            
        root = ET.fromstring(response.text)
        
        total = root.find('.//total')
        if total is not None and int(total.text) > 0:
            items = root.findall('.//item')
            items = items[:3]  # 최대 3개까지만 사용
            
            results = []
            for item in items:
                word_text = item.find('word').text
                sense = item.find('.//sense')
                pos = item.find('pos')
                pos_text = pos.text if pos is not None else "품사없음"
                
                result = {
                    "word": word_text,
                    "definition": sense.find('definition').text,
                    "pos": pos_text,
                    "difficulty": 1,
                    "consonants": get_word_consonants(word_text) if len(word_text) == 2 else ""
                }
                results.append(result)
            
            print(f"검색 완료: {word} ({len(results)}개)")
            return results
        else:
            print(f"검색 결과 없음: {word}")
                
    except Exception as e:
        print(f"오류 발생: {word}")
    
    return None

def main():
    API_KEY = "69E398DCF8DC5D67162322B845E64F47"
    
    words = read_words('cleaned_file2.txt')
    print(f"단어 목록 로드 완료: {len(words)}개")
    
    results = []
    
    for word in words:
        word_results = search_word(word, API_KEY)
        if word_results:
            results.extend(word_results)
        sleep(0.1)
    
    if results:
        df = pd.DataFrame(results)
        df.to_excel('2단계_단어_추출.xlsx', index=False)
        print(f"\n검색 완료. 총 {len(results)}개의 결과가 2단계_단어_추출.xlsx에 저장되었습니다.")
    else:
        print("\n저장할 결과가 없습니다.")

if __name__ == "__main__":
    main()
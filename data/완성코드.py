import requests
import pandas as pd
import numpy as np
import re
from time import sleep
import xml.etree.ElementTree as ET

# 한글 정제 함수
def clean_korean(text):
    """특수 문자를 제거하고 한글만 남깁니다."""
    text = re.sub(r'[-^]', '', text)  # 특수 문자 제거
    words = re.findall(r'[가-힣]+', text)  # 한글만 남김
    return words

def read_and_clean_words(file_path):
    """텍스트 파일에서 단어를 읽어와 정제하고 중복 제거합니다."""
    with open(file_path, 'r', encoding='utf-8') as file:
        lines = file.readlines()
    
    cleaned_words = set()
    for line in lines:
        cleaned_words.update(clean_korean(line))
    
    return sorted(cleaned_words)  # 중복 제거 후 정렬

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
        'num': 100,
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
            
            results = []
            for item in items:
                word_text = item.find('word').text.replace('-', '')
                sense = item.find('.//sense')
                definition = sense.find('definition').text
                if '→' not in definition:
                    result = {
                        "word": word_text,
                        "definition": definition,
                        "difficulty": 3,
                        "consonants": get_word_consonants(word_text) if len(word_text) == 2 else ""
                    }
                    results.append(result)
            
            print(f"검색 완료: {word} ({len(results)}개)")
            return results
        else:
            print(f"검색 결과 없음: {word}")
                
    except Exception as e:
        print(f"오류 발생: {word}, {str(e)}")
    
    return None

def main():
    API_KEY = "69E398DCF8DC5D67162322B845E64F47"
    
    input_file = '4단계.txt'
    vocab_level_file = '국어_기초_어휘_정리.xlsx'
    vocab_frequency_file = '단어_빈도_정보.xlsx'
    output_file = '최종_단어_난이도_분류.xlsx'
    
    # Step 1: 단어 읽기 및 정제
    words = read_and_clean_words(input_file)
    print(f"단어 목록 로드 및 정제 완료: {len(words)}개")
    
    # Step 2: API 검색
    results = []
    for word in words:
        word_results = search_word(word, API_KEY)
        if word_results:
            results.extend(word_results)
        sleep(0.01)
    
    if not results:
        print("API 검색 결과가 없습니다.")
        return
    
    # Step 3: 검색 결과를 데이터프레임으로 변환
    df = pd.DataFrame(results)
    
    # Step 4: 등급 정보 추가
    vocab_level_data = pd.read_excel(vocab_level_file)
    df = df.merge(vocab_level_data[['어휘', '등급']], left_on='word', right_on='어휘', how='left')
    df.drop(columns=['어휘'], inplace=True)
    
    # Step 5: 빈도 정보 추가
    vocab_frequency_data = pd.read_excel(vocab_frequency_file)
    df = df.merge(vocab_frequency_data, left_on='word', right_on='어휘', how='left')
    df['빈도'] = df['빈도'].fillna(0)  # 빈도수가 없는 경우 0으로 채움
    df.drop(columns=['어휘'], inplace=True)
    
    # Step 6: 단어 길이 계산
    df['word_length'] = df['word'].str.len()
    
    # Step 7: 난이도 조건 설정
    df = df[~((df['빈도'] == 0) & (df['word_length'] != 2))]  # 두 글자가 아닌 경우 제거
    df['난이도'] = np.where(
        (df['빈도'] == 0) & (df['word_length'] == 2),
        4,
        df['difficulty']  # 나머지는 difficulty 값을 그대로 사용
    )
    
    # Step 8: 최종 결과 저장
    df.to_excel(output_file, index=False)
    print(f"최종 결과가 {output_file}에 저장되었습니다.")

if __name__ == "__main__":
    main()

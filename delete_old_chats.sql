-- 기존 태그들의 채팅 데이터 삭제
DELETE FROM chats
WHERE
    JSON_CONTAINS(tag_names, '"교환/사이즈"')
    OR JSON_CONTAINS(tag_names, '"교환/컬러"')
    OR JSON_CONTAINS(tag_names, '"배송/운송장조회"')
    OR JSON_CONTAINS(tag_names, '"매장문의/신촌점/운영시간"')
    OR JSON_CONTAINS(tag_names, '"매장문의/안암점/운영시간"')
    OR JSON_CONTAINS(tag_names, '"매장문의/신촌점/프로모션"')
    OR JSON_CONTAINS(tag_names, '"매장문의/안암점/프로모션"')
    OR JSON_CONTAINS(tag_names, '"매장문의/신촌점/재고문의"')
    OR JSON_CONTAINS(tag_names, '"매장문의/안암점/재고문의"')
    OR JSON_CONTAINS(tag_names, '"매장문의/교환환불안내"')
    OR JSON_CONTAINS(tag_names, '"결제/환불/카카오페이"')
    OR JSON_CONTAINS(tag_names, '"결제/환불/무통장입금"')
    OR JSON_CONTAINS(tag_names, '"결제/환불/신용카드"');

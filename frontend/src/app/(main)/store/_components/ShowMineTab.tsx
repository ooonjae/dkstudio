import styled from 'styled-components'

interface MineTabStyleProp {
  $showMode: boolean
}

const MineTabWrapper = styled.div`
  display: flex;
  width: 300px;
`

const MineTab = styled.div<MineTabStyleProp>`
  border-bottom: solid ${(props) => (props.$showMode ? 'black' : 'white')};
  flex: 1;
  text-align: center;
  padding: 10px;
`

function ShowMineTab({ showMine, handleMineChange }: any) {
  return (
    <MineTabWrapper>
      <MineTab $showMode={!showMine} onClick={() => handleMineChange(false)}>
        전체
      </MineTab>
      <MineTab $showMode={showMine} onClick={() => handleMineChange(true)}>
        구매한 상품
      </MineTab>
    </MineTabWrapper>
  )
}

export default ShowMineTab

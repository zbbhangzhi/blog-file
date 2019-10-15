package com.zbb.lizi.algorithm;

import java.util.Stack;

/**
 * @Author tiancha
 * @create 2019/10/10 19:57
 */
public class DFS {
    public static void main(String[] args) {
        //定义数组存储整个地图
        int[][] map ={{0,1,0,0,0},{0,1,0,1,0},{0,0,0,0,0},{0,1,1,1,0},{0,0,0,1,0}};

        int[][] dir = {{1,0},{0,1}};//定义两个方向横着走或者竖着走（这里我们定义只走这两个方向，当然也可以定义多个方向）
        Stack<Node> stack = new Stack<Node>();//定义一个栈，保存路径
        int[][] visited = new int[5][5];//标记是否被访问，这个要和Map大小对应
        Node start = new Node(0,0);//定义起始位置
        Node end = new Node(4,4);//定义终点位置
        visited[start.x][start.y] = 1;
        stack.push(start);//将起始点加入队列
        while(!stack.isEmpty())//如果队列为空了还没有找到解，说明就没有通路
        {
            boolean flag = false;//标记是否找到了一个方向
            Node pek = stack.peek();//获取栈顶元素，注意不需要出栈 后进先出
            if(pek.x==end.x&&pek.y==end.y){//如果到达目的地，则跳出循环
                break;
            }else{
                for (int i = 0; i < 2; i++) {//循环两个方向
                    Node nbr = new Node(pek.x + dir[i][0],pek.y + dir[i][1]);//找到当前位置的邻居位置坐标并判断是否合法
                    if(nbr.x>=0&&nbr.x<5&&nbr.y>=0&&nbr.y<5&&map[nbr.x][nbr.y]==0&&visited[nbr.x][nbr.y]==0){//判断邻居节点是否合法
                        stack.push(nbr);//合法将邻居位置加入栈     **这里是关键，也就是递归思想
                        visited[nbr.x][nbr.y] = 1;//并标志为1
                        flag = true;
                        break;//找到了就停止循环，顺着这个方向一直搜索
                    }

                }
                if(flag){//找到了方向，就不用执行下面的出栈，沿着这个方向一直搜下去
                    continue;
                }
                stack.pop();//如果两个方向都不能通过，则出栈。
            }
        }
        //这里可以加个判断，如果stack为空，说明没有解方案

        Stack<Node> stkRev = new Stack<Node>();//将路径反过来，因为栈中输出的路径是反的
        while (!stack.isEmpty()) {
            stkRev.push(stack.pop());
        }
        while (!stkRev.isEmpty()) {
            System.out.println("(" + stkRev.peek().x + "," + stkRev.peek().y + ")");
            stkRev.pop();
        }

    }
}

//定义数据结构
class Node{
    int y;
    int x;
    Node(int x,int y){
        this.x = x;
        this.y = y;
    }
}

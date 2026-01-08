import React, { useEffect, useState } from "react";

//Added
import axios from "axios";
import { Link, useParams } from "react-router-dom";


export default function Home() {

    const [users, setUsers] = useState([])

    const {id} = useParams()

    useEffect(()=>{
        loadUsers();
    }, []);


    const API_URL = process.env.REACT_APP_API_URL;
    const loadUsers=async()=>{
        const result=await axios.get(`${API_URL}/users`);
        setUsers(result.data);
    }


    const deleteUser=async(id)=>{
        await axios.delete(`${API_URL}/user/${id}`);
        loadUsers();
    }



  return (
    <div className='container'> 
        <div className='py-4'>

            <table className="table border shadow">
            <thead>
                <tr>
                    <th scope="col">#</th>
                    <th scope="col">Name</th>
                    <th scope="col">Username</th>
                    <th scope="col">Email</th>
                    <th scope="col">Action</th>
                </tr>
            </thead>
            <tbody>
                {
                    users.map( (users, index)=>(
                        <tr>
                            <th scope="row" key={index}>{users.id}</th>
                            <td>{users.name}</td>
                            <td>{users.username}</td>
                            <td>{users.email}</td>
                            <td>
                                <Link className="btn btn-primary mx-2" to={`/viewuser/${users.id}`}>View</Link>
                                <Link className="btn btn-outline-primary mx-2" to={`/edituser/${users.id}`}>Edit</Link>
                                <button className="btn btn-danger mx-2" onClick={()=> deleteUser(users.id)}>Delete</button>
                            </td>
                        </tr>
                    ) )
                }
               
            </tbody>
            </table>

        </div>
    </div>
  )
}

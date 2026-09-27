import { BrowserRouter, Route, Routes } from 'react-router-dom'
import Navbar from './components/Navbar'
import ProtectedRoute from './components/ProtectedRoute'
import { AuthProvider } from './context/AuthContext'
import AdminModeration from './pages/AdminModeration'
import AdminVisits from './pages/AdminVisits'
import Home from './pages/Home'
import ListingActivity from './pages/ListingActivity'
import ListingDetail from './pages/ListingDetail'
import Login from './pages/Login'
import MyListings from './pages/MyListings'
import MyVisits from './pages/MyVisits'
import PostProperty from './pages/PostProperty'
import Register from './pages/Register'

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Navbar />
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/listings/:id" element={<ListingDetail />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route
            path="/post-property"
            element={
              <ProtectedRoute role="OWNER">
                <PostProperty />
              </ProtectedRoute>
            }
          />
          <Route
            path="/my-listings"
            element={
              <ProtectedRoute role="OWNER">
                <MyListings />
              </ProtectedRoute>
            }
          />
          <Route
            path="/my-listings/:id"
            element={
              <ProtectedRoute role="OWNER">
                <ListingActivity />
              </ProtectedRoute>
            }
          />
          <Route
            path="/my-visits"
            element={
              <ProtectedRoute>
                <MyVisits />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin"
            element={
              <ProtectedRoute role="ADMIN">
                <AdminModeration />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/visits"
            element={
              <ProtectedRoute role="ADMIN">
                <AdminVisits />
              </ProtectedRoute>
            }
          />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
